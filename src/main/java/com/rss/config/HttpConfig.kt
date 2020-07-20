package com.rss.config

import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration open class HttpConfig {
    @Bean open fun okHTTPClient() : OkHttpClient {
        return OkHttpClient()
    }
}