package com.rss.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "messaging")
open class MessagingProperties {
    var url : String? = null
    var token : String? = null
    var rssPublishSubject : String? = null
}