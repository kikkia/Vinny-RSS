package com.rss.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "auth")
open class AuthProperties {
    var clientIps: List<String>? = null
    var clientToken: String? = null
    var youtubeToken: String? = null
}