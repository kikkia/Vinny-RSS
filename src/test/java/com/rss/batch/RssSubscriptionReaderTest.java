package com.rss.batch;

import com.rss.clients.ChanClient;
import com.rss.config.batch.BatchConfiguration;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssProvider;
import com.rss.service.MetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RssSubscriptionReaderTest {
    @Test
    void eachJobReadsAtMostOneBoardEvenWhenMoreAreAvailable() {
        RssSubscriptionDTO subscription = new RssSubscriptionDTO(7, "sci", RssProvider.CHAN.getValue(),
                Instant.EPOCH, Instant.EPOCH, null);
        int[] reads = {0};
        RssSubscriptionRepository repository = new RssSubscriptionRepository() {
            @Override
            public RssSubscriptionDTO getNextSubscription(RssProvider provider) {
                assertEquals(RssProvider.CHAN, provider);
                reads[0]++;
                return subscription;
            }
        };
        MetricsService metrics = new MetricsService(null) {
            @Override
            public void markReadExectution(RssProvider provider, long sinceAttempt, long sinceComplete) {
                assertEquals(RssProvider.CHAN, provider);
            }
        };
        RssSubscriptionReader reader = new RssSubscriptionReader(repository, metrics, RssProvider.CHAN);

        assertSame(subscription, reader.read());
        assertNull(reader.read());
        assertNull(reader.read());
        assertEquals(1, reads[0]);
    }

    @Test
    void schedulesBoardScansWithFiveSecondsBetweenLaunches() throws Exception {
        Scheduled scheduled = BatchConfiguration.class.getMethod("launchChanRssScan").getAnnotation(Scheduled.class);
        assertEquals(5000, ChanClient.SCAN_INTERVAL_MILLIS);
        assertEquals(ChanClient.SCAN_INTERVAL_MILLIS, scheduled.fixedDelay());
        assertEquals(-1, scheduled.fixedRate());
    }
}
