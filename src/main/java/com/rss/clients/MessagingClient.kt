package com.rss.clients

import com.google.gson.GsonBuilder
import com.rss.config.properties.MessagingProperties
import com.rss.model.RssUpdate
import io.nats.client.Connection
import org.springframework.stereotype.Component

@Component open class MessagingClient(val properties: MessagingProperties, private val connection: Connection) {
    fun sendRssUpdate(update: RssUpdate) {
        val encodedUpdate = GsonBuilder().create().toJson(update)
        connection.publish(properties.rssPublishSubject, encodedUpdate.toByteArray())
    }
}