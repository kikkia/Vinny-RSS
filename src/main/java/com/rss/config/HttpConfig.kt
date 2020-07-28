package com.rss.config

import com.rss.utils.UserAgentInterceptor
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class HttpConfig {
    @Bean
    open fun client(): OkHttpClient {
        val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.89 Safari/537.36 Vivaldi/3.1."
        return OkHttpClient.Builder()
                .addInterceptor(UserAgentInterceptor(USER_AGENT))
                .build()
    }
}