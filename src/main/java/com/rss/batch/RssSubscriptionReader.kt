package com.rss.batch

import com.rss.db.dao.RssSubscriptionRepository
import com.rss.db.model.RssSubscriptionDTO
import com.rss.model.RssProvider
import com.rss.service.MetricsService
import com.rss.utils.DislogLogger
import org.springframework.batch.item.ItemReader
import java.time.Instant

class RssSubscriptionReader(private val repository: RssSubscriptionRepository,
                            private val metricsService: MetricsService,
                            private val provider: RssProvider) : ItemReader<RssSubscriptionDTO?> {
    private val logger = DislogLogger(this.javaClass)
    override fun read(): RssSubscriptionDTO? {
        val dto = repository.getNextSubscription(provider)
        if (dto == null) {
            // logger.info("No feed ready for sync")
        } else {
            //logger.debug("Starting rss sync for subject " + dto.url)
            metricsService.markReadExectution(provider,
                    Instant.now().toEpochMilli() - dto.lastScanAttempt.toEpochMilli(),
                    Instant.now().toEpochMilli() - dto.lastScanComplete.toEpochMilli())
        }
        return dto
    }
}