package com.rss.config.properties


import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "network")
open class NetworkingProperties {
    var ipv6Cidr : String? = null
}