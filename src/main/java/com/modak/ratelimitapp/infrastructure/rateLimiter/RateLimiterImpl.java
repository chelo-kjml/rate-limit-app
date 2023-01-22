package com.modak.ratelimitapp.infrastructure.rateLimiter;

import com.modak.ratelimitapp.domain.models.RateLimitDefinition;
import com.modak.ratelimitapp.domain.models.exception.NoSuchRateLimitTypeException;
import com.modak.ratelimitapp.domain.models.exception.RateLimitReachedException;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiterImpl implements RateLimiter {

    private final HikariDataSource dataSource;
    @Getter
    private final Map<String, RateLimitDefinition> rateLimitsByType = new HashMap<>();

    private static final String RATE_LIMIT_TYPE_TABLE = "rate_limit_type";
    private static final String ID_COLUMN = "id";
    private static final String NAME_COLUMN = "name";
    private static final String TIME_WINDOW_COLUMN = "time_window_in_seconds";
    private static final String MAX_THRESHOLD_COLUMN = "max_threshold";
    private static final String FETCH_RATE_LIMIT_TYPES_QUERY = String.format(
           "SELECT %s, %s, %s, %s FROM %s", ID_COLUMN, NAME_COLUMN, TIME_WINDOW_COLUMN, MAX_THRESHOLD_COLUMN,
            RATE_LIMIT_TYPE_TABLE);

    private static final String USER_OPERATION_TABLE = "user_operation";
    private static final String USER_ID_COLUMN = "user_id";
    private static final String RATE_LIMIT_TYPE_ID_COLUMN = "rate_limit_type_id";
    private static final String INSERTED_AT_COLUMN = "inserted_at";

    @PostConstruct
    public void initRateLimitTypeCache() throws Exception {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(FETCH_RATE_LIMIT_TYPES_QUERY);
        ) {

            log.info("Initializing rateLimitType cache...");

            var rs = ps.executeQuery();

            while (rs.next()) {
                rateLimitsByType.put(rs.getString(NAME_COLUMN), RateLimitDefinition.builder()
                                .id(rs.getLong(ID_COLUMN))
                                .name(rs.getString(NAME_COLUMN))
                                .windowInSeconds(rs.getInt(TIME_WINDOW_COLUMN))
                                .maxThreshold(rs.getInt(MAX_THRESHOLD_COLUMN))
                                .build());
            }

            rs.close();

            log.info("RateLimitType cache initialized successfully.");

        } catch (Exception ex) {
            log.error("Failed to initialize rateLimitType cache.");
            throw ex;
        }
    }


    @Override
    public void checkRateLimitAndExecuteOperation(String type, String userId, Runnable runnable) throws Exception {
        var limitDefinition = rateLimitsByType.get(type);

        if (Objects.isNull(limitDefinition)) {
            throw new NoSuchRateLimitTypeException(String.format("Provided message type '%s' does not exist.", type));
        }

        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            conn.createStatement().executeUpdate(String.format("LOCK TABLE %s WRITE, %s AS mu READ;",
                    USER_OPERATION_TABLE, USER_OPERATION_TABLE));

            if (getCounter(limitDefinition.getId(), userId, limitDefinition.getWindowInSeconds(), conn) < limitDefinition.getMaxThreshold()) {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    log.error("Unexpected error. Exception: {}.", ex.getMessage());
                    conn.createStatement().executeUpdate("UNLOCK TABLES;");
                    throw ex;
                }

                incrementCounterAndReleaseLock(limitDefinition.getId(), userId, conn);
            } else {
                conn.createStatement().executeUpdate("UNLOCK TABLES;");
                log.warn("Reached rate limit for user_id {} and operation type {}.", userId, type);
                throw new RateLimitReachedException(String.format("Rate limit reached for type: %s", type));
            }
        }
    }

    private void incrementCounterAndReleaseLock(Long rateLimitTypeId, String userId, Connection conn) throws Exception {
        try {

            var ps = conn.prepareStatement(
                    String.format("INSERT INTO %s (%s, %s) VALUES (?,?);",
                            USER_OPERATION_TABLE, USER_ID_COLUMN, RATE_LIMIT_TYPE_ID_COLUMN)
            );
            ps.setString(1, userId);
            ps.setLong(2, rateLimitTypeId);

            ps.executeUpdate();

        } catch (Exception ex) {
            log.error("ERROR trying to insert message type_id '{}' for user_id '{}'. Error: {}.",
                    rateLimitTypeId, userId, ex.getMessage());
            throw ex;
        } finally {
            conn.createStatement().executeUpdate("unlock tables;");
        }
    }

    private int getCounter(Long rateLimitTypeId, String userId, double windowInSeconds, Connection conn) throws Exception{
        try {

            var ps = conn.prepareStatement(String.format("""
                    SELECT count(*) as count from %s as mu
                    where %s = ? and %s = ? and %s >= now() - interval ? second
                    """, USER_OPERATION_TABLE, USER_ID_COLUMN, RATE_LIMIT_TYPE_ID_COLUMN, INSERTED_AT_COLUMN));
            ps.setString(1, userId);
            ps.setLong(2, rateLimitTypeId);
            ps.setDouble(3, windowInSeconds);

            var rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            } else {
                return 0;
            }

        } catch (Exception ex) {
            log.error("ERROR trying to check operation rate for rate_limit_type_id: {} and user_id {}. Error: {}.",
                    rateLimitTypeId, userId, ex.getMessage());
            throw ex;
        }
    }
}
