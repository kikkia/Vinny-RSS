package com.rss.batch

import com.rss.db.dao.RssSubscriptionRepository
import com.rss.db.model.RssSubscriptionDTO
import com.rss.model.RssProvider
import com.rss.utils.DislogLogger
import org.springframework.batch.item.ItemReader

class RssSubscriptionReader(private val repository: RssSubscriptionRepository, private val provider: RssProvider) : ItemReader<RssSubscriptionDTO?> {
    private val logger = DislogLogger(this.javaClass)
    override fun read(): RssSubscriptionDTO? {
        val dto = repository.getNextSubscription(provider)
        if (dto == null) {
            // logger.info("No feed ready for sync")
        } else {
            logger.debug("Starting rss sync for subject " + dto.url)
        }
        return dto
    }
}