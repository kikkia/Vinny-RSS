package com.rss.batch

import com.rss.clients.MessagingClient
import com.rss.model.RssUpdate
import org.springframework.batch.item.ItemWriter

class RssSubscriptionWriter(private val messagingClient: MessagingClient) : ItemWriter<List<RssUpdate?>> {
    @Throws(Exception::class)
    override fun write(list: List<List<RssUpdate?>>) {
        if (list.isEmpty())
            return
        for (updateList in list) {
            for (update in updateList) {
                messagingClient.sendRssUpdate(update!!)
            }
        }
        print(list)
    }
}