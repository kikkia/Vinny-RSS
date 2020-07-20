package com.rss.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "dislog")
open class DislogProperties {
    var isEnabled = false
    var hostIdentifier: String? = null
    var username: String? = null
    var avatarUrl: String? = null
    var debugWebhooks: List<String>? = null
    var traceWebhooks: List<String>? = null
    var infoWebhooks: List<String>? = null
    var warnWebhooks: List<String>? = null
    var errorWebhooks: List<String>? = null
}