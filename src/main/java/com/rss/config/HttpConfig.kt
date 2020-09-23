package com.rss.config

import com.rss.clients.networking.RotatatingIpV6SocketFactory
import com.rss.config.properties.NetworkingProperties
import com.rss.utils.UserAgentInterceptor
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.net.SocketFactory

@Configuration
open class HttpConfig {
    @Bean
    open fun client(networkingProperties: NetworkingProperties): OkHttpClient {
        val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.89 Safari/537.36 Vivaldi/3.1."
        val socketFactory = if (networkingProperties.ipv6Cidr == null) SocketFactory.getDefault()
            else RotatatingIpV6SocketFactory.byIpv6CIDR(networkingProperties.ipv6Cidr!!)
        return OkHttpClient.Builder()
                .addInterceptor(UserAgentInterceptor(USER_AGENT))
                .socketFactory(socketFactory!!)
                .build()
    }
}