package com.rss.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "db")
open class DatasourceProperties {
    var uri: String? = null
    var username: String? = null
    var password: String? = null
    var schema: String? = null
}