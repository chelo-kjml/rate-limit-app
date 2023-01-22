package com.modak.ratelimitapp.infrastructure.db;

import lombok.Data;

@Data
public class JdbcConfigProperties {

    String poolName;
    String jdbcUrl;
    String username;
    String password;
    String connectionTestQuery;
    int minimumIdle;
    int validationTimeout;
    boolean registerMbeans;
    int maximumPoolSize;

}