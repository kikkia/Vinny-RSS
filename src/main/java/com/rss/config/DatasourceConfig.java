package com.rss.config;

import com.rss.config.properties.DatasourceProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatasourceConfig {

    @Bean
    public HikariDataSource hikariDataSource(HikariConfig config) {
        return new HikariDataSource(config);
    }

    @Bean
    HikariConfig hikariConfig(DatasourceProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setPassword(properties.getPassword());
        config.setUsername(properties.getUsername());
        config.setJdbcUrl("jdbc:mysql://" + properties.getUri());
        config.setSchema(properties.getSchema());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        return config;
    }
}
