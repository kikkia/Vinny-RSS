package com.rss.config

import com.rss.config.properties.DatasourceProperties
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class DatasourceConfig {
    @Bean
    open fun hikariDataSource(config: HikariConfig?): HikariDataSource {
        return HikariDataSource(config)
    }

    @Bean
    open fun hikariConfig(properties: DatasourceProperties): HikariConfig {
        val config = HikariConfig()
        config.password = properties.password
        config.username = properties.username
        config.jdbcUrl = "jdbc:mysql://" + properties.uri + "/" + properties.schema
        config.schema = properties.schema
        config.maximumPoolSize = 10
        config.minimumIdle = 2
        return config
    }
}