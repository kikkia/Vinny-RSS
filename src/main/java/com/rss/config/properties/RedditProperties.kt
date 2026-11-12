package com.rss.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@ConfigurationProperties(prefix = "reddit")
open class RedditProperties {
    var clientId: String = ""
    var userAgent: String = ""
    // Reused for every token renewal. Configure explicitly to retain it across restarts.
    var deviceId: String = UUID.randomUUID().toString().replace("-", "").take(30)
}
