package com.modak.ratelimitapp.infrastructure.db;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties
public class DatabaseConfig {

    @Bean
    @ConfigurationProperties("common.datasource")
    public HikariConfig datasourceConfig() {
        return new HikariConfig();
    }
    @Bean
    public HikariDataSource mysqlDatasource(final HikariConfig datasourceConfig) {
        return new HikariDataSource(datasourceConfig);
    }

}
