package com.modak.ratelimitapp.notification;

import com.modak.ratelimitapp.domain.service.notification.NotificationService;
import com.modak.ratelimitapp.domain.service.notification.NotificationServiceImpl;
import com.modak.ratelimitapp.infrastructure.gateway.Gateway;
import com.modak.ratelimitapp.infrastructure.gateway.GatewayImpl;
import com.modak.ratelimitapp.infrastructure.rateLimiter.RateLimiterImpl;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public abstract class BaseTestContainer {

    protected static final GenericContainer MYSQL_CONTAINER = new MySQLContainer(DockerImageName.parse("mysql:8.0.23"))
            .withEnv("TZ","America/Argentina/Buenos_Aires")
            .withReuse(true);

    static {
        MYSQL_CONTAINER.start();
    }

    protected HikariDataSource hikariDataSource;
    protected RateLimiterImpl rateLimiter;
    protected NotificationService notificationService;


    @BeforeAll
    public void initNotificationService() throws Exception {

        String address = MYSQL_CONTAINER.getHost();
        Integer port = MYSQL_CONTAINER.getFirstMappedPort();

        var jdbcUrl = String.format("jdbc:mysql://%s:%s/test?useSSL=false", address, port);

        var hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("mysql-pool");
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername("test");
        hikariConfig.setPassword("test");
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setMinimumIdle(10);
        hikariConfig.setRegisterMbeans(false);
        hikariConfig.setMaximumPoolSize(15);

        hikariDataSource = new HikariDataSource(hikariConfig);

        try (var conn = hikariDataSource.getConnection()) {
            createTables(conn);
        } catch (Exception ex) {
            log.error("Unable to init container and create tables. Exception: {}", ex.getMessage());
            throw ex;
        }

        Gateway gateway = new GatewayImpl();
        rateLimiter = new RateLimiterImpl(hikariDataSource);

        rateLimiter.initRateLimitTypeCache();
        notificationService = new NotificationServiceImpl(gateway, rateLimiter);
    }

    private void createTables(Connection conn) throws SQLException {
        conn.createStatement().executeUpdate("DROP TABLE IF EXISTS user_operation;");
        conn.createStatement().executeUpdate("DROP TABLE IF EXISTS rate_limit_type;");

        conn.createStatement().executeUpdate("""
                create table rate_limit_type(
                    id bigint not null primary key auto_increment,
                    name varchar(45) not null unique,
                    time_window_in_seconds int not null,
                    max_threshold int
                );
                """);

        conn.createStatement().executeUpdate("""
                create table user_operation(
                    user_id varchar(140) not null,
                    rate_limit_type_id bigint not null,
                    inserted_at timestamp(6) not null default current_timestamp(6),
                    primary key (user_id, rate_limit_type_id, inserted_at)
                );
                """);

        conn.createStatement().executeUpdate("""
                insert into rate_limit_type (name, time_window_in_seconds, max_threshold)
                VALUES ('status', 60, 2),
                       ('news', 86400, 1),
                       ('marketing', 3600, 3),
                       ('ping', 3, 3);
                """);
    }
}
