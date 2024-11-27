package com.rss.batch.processors;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssProvider;
import com.rss.model.RssUpdate;
import com.rss.service.MetricsService;
import com.rss.utils.DislogLogger;
import com.rss.utils.RssUtils;
import org.springframework.batch.item.ItemProcessor;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BlueSkyRssProcessor implements ItemProcessor<RssSubscriptionDTO, List<RssUpdate>> {
    private RssSubscriptionRepository repository;
    private MetricsService metricsService;
    private DislogLogger logger = new DislogLogger(this.getClass());

    public BlueSkyRssProcessor(RssSubscriptionRepository repository, MetricsService metricsService) {
        this.repository = repository;
        this.metricsService = metricsService;
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        String url = RssUtils.Companion.getBlueSkyUrl(rssSubscriptionDTO.getUrl());
        Instant lastScan = rssSubscriptionDTO.getLastScanComplete();

        SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
        List<RssChannelSubscriptionDTO> subs = repository.getChannelSubsForSubcriptionId(rssSubscriptionDTO.getId());
        ArrayList<RssUpdate> toUpdate = new ArrayList<>();
        if (feed.getEntries().isEmpty()) {
            logger.warn("EMPTY BLUESKY RSS FEED FOUND");
            metricsService.markExecutionFailed(RssProvider.BLUESKY);
        }
        String subject = feed.getTitle();
        System.out.println("Just scanned: " + subject);

        for (SyndEntry entry : feed.getEntries()) {
            Instant posted = entry.getPublishedDate().toInstant();
            if (posted.isAfter(lastScan) && Instant.now().minusSeconds(posted.getEpochSecond()).getEpochSecond() < 7200) {

                for (RssChannelSubscriptionDTO dto : subs) {
                    toUpdate.add(new RssUpdate(
                            rssSubscriptionDTO.getId(),
                            dto.getChannelId(),
                            entry.getLink(),
                            rssSubscriptionDTO.getProvider(),
                            rssSubscriptionDTO.getUrl(),
                            subject
                    ));
                }
            }
        }
        // If empty do not update completed timestamp, this is due to slow updates of the consumed feeds.
        if (toUpdate.isEmpty()) {
            return null;
        }
        if (!repository.updateLastScanComplete(rssSubscriptionDTO.getId())) {
            logger.error("Failed to mark the last completed time, failing job");
            return null;
        }

        return toUpdate;
    }
}
