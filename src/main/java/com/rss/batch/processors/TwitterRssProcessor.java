package com.rss.batch.processors;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssUpdate;
import com.rss.utils.DislogLogger;
import com.rss.utils.RssUtils;
import org.slf4j.MDC;
import org.springframework.batch.item.ItemProcessor;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TwitterRssProcessor implements ItemProcessor<RssSubscriptionDTO, List<RssUpdate>> {

    private RssSubscriptionRepository repository;
    private DislogLogger logger = new DislogLogger(this.getClass());
    private String nitterPath;

    public TwitterRssProcessor(RssSubscriptionRepository repository, String nitterPath) {
        this.repository = repository;
        this.nitterPath = nitterPath;
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        String url = RssUtils.Companion.getTwitterUrl(rssSubscriptionDTO.getUrl(), nitterPath);
        Instant lastScan = rssSubscriptionDTO.getLastScanComplete();

        SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
        List<RssChannelSubscriptionDTO> subs = repository.getChannelSubsForSubcriptionId(rssSubscriptionDTO.getId());
        ArrayList<RssUpdate> toUpdate = new ArrayList<>();
        if (feed.getEntries().isEmpty()) {
            logger.warn("EMPTY TWITTER RSS FEED FOUND");
        }
        for (SyndEntry entry : feed.getEntries()) {
            Instant posted = entry.getPublishedDate().toInstant();
            if (posted.isAfter(lastScan)) {
                String subject = rssSubscriptionDTO.getUrl().replace("@", "");
                boolean rt = entry.getTitle().startsWith("RT by @" + subject);
                // Sometimes issues where the wrong user tweets come up with rss is happening
                // This will ensure non RTs are from the user. (I suspect a nitter bug)
                if (!rt && !entry.getLink().contains(subject)) {
                    try (MDC.MDCCloseable a = MDC.putCloseable("twitter subject", rssSubscriptionDTO.getUrl());
                            MDC.MDCCloseable b = MDC.putCloseable("actual subject", entry.getLink());
                            MDC.MDCCloseable c = MDC.putCloseable("title", entry.getTitle())){
                        logger.error("Hit wrong subject for non RT update!");
                    }
                    break;
                }

                for (RssChannelSubscriptionDTO dto : subs) {
                    toUpdate.add(new RssUpdate(
                            rssSubscriptionDTO.getId(),
                            dto.getChannelId(),
                            entry.getLink().replace("nitter.net", "twitter.com"),
                            rssSubscriptionDTO.getProvider(),
                            rt ? "**VINNY**RT@" + subject : "@" + subject,
                            rssSubscriptionDTO.getUrl()
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
        repository.updateLastScanComplete(rssSubscriptionDTO.getId());
        return toUpdate;
    }
}
