package com.rss.config

import com.kikkia.dislog.api.DislogClient
import com.kikkia.dislog.models.LogLevel
import com.rss.config.properties.DislogProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
open class DislogConfig {
    @Bean
    open fun dislogClient(properties: DislogProperties): DislogClient? {
        if (!properties.isEnabled) return null
        val builder = DislogClient.Builder()
                .setUsername(properties.username!!)
                .setIdentifier(properties.hostIdentifier!!)
                .setAvatarUrl(properties.avatarUrl!!)
        val hookMap = HashMap<LogLevel, List<String>?>()
        hookMap[LogLevel.DEBUG] = properties.debugWebhooks
        hookMap[LogLevel.TRACE] = properties.traceWebhooks
        hookMap[LogLevel.INFO] = properties.infoWebhooks
        hookMap[LogLevel.WARN] = properties.warnWebhooks
        hookMap[LogLevel.ERROR] = properties.errorWebhooks
        for ((key, value) in hookMap) {
            for (url in value!!) {
                builder.addWebhook(key, url)
            }
        }
        return builder.build()
    }
}