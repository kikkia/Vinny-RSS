package com.rss.batch

import com.rss.clients.MessagingClient
import com.rss.model.RssProvider
import com.rss.model.RssUpdate
import com.rss.utils.DislogLogger
import org.springframework.batch.item.ItemWriter

class RssSubscriptionWriter(private val messagingClient: MessagingClient) : ItemWriter<List<RssUpdate?>> {

    private val logger = DislogLogger(this.javaClass)
    @Throws(Exception::class)
    override fun write(list: List<List<RssUpdate?>>) {
        if (list.isEmpty())
            return
        for (updateList in list) {
            for (update in updateList) {
                logger.info("Publishing update for ${update!!.displayName} + ${update.url}")
                messagingClient.sendRssUpdate(update)
            }
        }
        print(list)
    }
}