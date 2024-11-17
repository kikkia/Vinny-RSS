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
    private var used = false
    override fun read(): RssSubscriptionDTO? {
        // A hack to make each step correlate to one scan, before it would scan more than indended on each interval
        if (used) {
            return null
        }

        val dto = repository.getNextSubscription(provider)
        if (dto == null) {
            // logger.info("No feed ready for sync")
        } else {
            //logger.debug("Starting rss sync for subject " + dto.url)
            metricsService.markReadExectution(provider,
                    Instant.now().toEpochMilli() - dto.lastScanAttempt.toEpochMilli(),
                    Instant.now().toEpochMilli() - dto.lastScanComplete.toEpochMilli())
        }
        used = true
        return dto
    }
}