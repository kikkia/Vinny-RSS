package com.rss.config

import com.rss.config.properties.MessagingProperties
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration open class MessagingConfig(private val propeties: MessagingProperties) {
    @Bean open fun natsOptions() : Options {
        return Options.Builder().server(propeties.url).token(propeties.token!!.toCharArray()).build()
    }

    @Bean open fun natsConnection(options: Options) : Connection {
        return Nats.connect(options)
    }
}