package com.modak.ratelimitapp.notification;
import com.modak.ratelimitapp.domain.models.exception.RateLimitReachedException;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public class NotificationServiceTest extends BaseTestContainer {


    @Test
    public void singleUserTest() throws Exception {
        var userId = "user_id_1";
        var statusMessageType = "status";

        try {
            notificationService.send(statusMessageType, userId, "some message 1");
            notificationService.send(statusMessageType, userId, "some message 2");
            notificationService.send(statusMessageType, userId, "some message 3");
            notificationService.send(statusMessageType, userId, "some message 4");
        } catch (Exception ex) {
            assertInstanceOf(RateLimitReachedException.class, ex);
        }

        assertMessagesCount(userId, rateLimiter.getRateLimitsByType().get(statusMessageType).getId(), 2);

        var pingMessageType = "ping";
        try {
            notificationService.send(pingMessageType, userId, "some message 1");
            notificationService.send(pingMessageType, userId, "some message 2");
            notificationService.send(pingMessageType, userId, "some message 3");
            notificationService.send(pingMessageType, userId, "some message 4");
            notificationService.send(pingMessageType, userId, "some message 5");
        } catch (Exception ex) {
            assertInstanceOf(RateLimitReachedException.class, ex);
        }

        Thread.sleep(4000);

        notificationService.send(pingMessageType, userId, "some message 4");
        notificationService.send(pingMessageType, userId, "some message 5");
        notificationService.send(pingMessageType, userId, "some message 6");

        assertMessagesCount(userId, rateLimiter.getRateLimitsByType().get(pingMessageType).getId(), 6);

    }

    @Test
    public void concurrencyTest() throws SQLException {
        generateRandomData();

        assertCountGroupByUserIdAndRateLimitType(
                rateLimiter.getRateLimitsByType().get("status").getId(),
                rateLimiter.getRateLimitsByType().get("status").getMaxThreshold()
        );

        assertCountGroupByUserIdAndRateLimitType(
                rateLimiter.getRateLimitsByType().get("news").getId(),
                rateLimiter.getRateLimitsByType().get("news").getMaxThreshold()
        );

        assertCountGroupByUserIdAndRateLimitType(
                rateLimiter.getRateLimitsByType().get("marketing").getId(),
                rateLimiter.getRateLimitsByType().get("marketing").getMaxThreshold()
        );

        try(var conn = hikariDataSource.getConnection()) {
            var rs = conn.createStatement().executeQuery("""
                    select count(*) as count
                    from user_operation;
                    """);
            rs.next();
            assertEquals(90, rs.getInt(1));
        }
    }

    private void assertCountGroupByUserIdAndRateLimitType(long rateLimitTypeId, int maxThreshold) throws SQLException{
        try(var conn = hikariDataSource.getConnection()) {
            var ps = conn.prepareStatement("""
                    select user_id, rate_limit_type_id, count(*) as count
                    from user_operation
                    where rate_limit_type_id = ?
                    group by user_id, rate_limit_type_id
                    having count > ?;
                    """);
            ps.setLong(1, rateLimitTypeId);
            ps.setInt(2, maxThreshold);

            var rs = ps.executeQuery();
            assertFalse(rs.isBeforeFirst());
        }
    }

    private void generateRandomData(){
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<String> userIdsForTesting = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            var userId = String.format("user_id_%s", i);
            userIdsForTesting.add(userId);
        }

        userIdsForTesting.forEach(userId -> {
            for (int j = 0; j < 10; j++) {
                pool.execute(() -> {
                    try {
                        notificationService.send("status",
                                userId,
                                "Some message for userId " + userId
                        );
                    } catch (Exception ignore){}
                });
            }

            for (int j = 0; j < 10; j++) {
                pool.execute(() -> {
                    try {
                        notificationService.send("news",
                                userId,
                                "Some message for userId " + userId
                        );
                    } catch (Exception ignore){}
                });
            }

            for (int j = 0; j < 10; j++) {
                pool.execute(() -> {
                    try {
                        notificationService.send("marketing",
                                userId,
                                "Some message for userId " + userId
                        );
                    } catch (Exception ignore){}
                });
            }
        });
    }

    private void assertMessagesCount(String userId, long rateLimitTypeId, int expectedCount) throws SQLException {
        try (var conn = hikariDataSource.getConnection()) {
            var ps = conn.prepareStatement("""
                            select count(*) from user_operation where user_id = ? and rate_limit_type_id = ?
                            """);
            ps.setString(1, userId);
            ps.setLong(2, rateLimitTypeId);
            var rs = ps.executeQuery();

            rs.next();
            assertEquals(expectedCount, rs.getInt(1));
        }
    }

    @AfterEach
    public void deleteOperations() throws SQLException {
        try (var conn = hikariDataSource.getConnection()) {
            conn.createStatement().executeUpdate("truncate table user_operation;");
        }
    }
}
