package com.rss.batch;

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
                boolean rt = entry.getTitle().startsWith("RT by");
                for (RssChannelSubscriptionDTO dto : subs) {
                    toUpdate.add(new RssUpdate(
                            rssSubscriptionDTO.getId(),
                            dto.getChannelId(),
                            entry.getLink().replace("nitter.net", "twitter.com"),
                            rssSubscriptionDTO.getProvider(),
                            rt ? "**VINNY**RT@" + rssSubscriptionDTO.getUrl() : "@" + rssSubscriptionDTO.getUrl(),
                            rssSubscriptionDTO.getUrl()
                            ));
                }
            }
        }
        // If empty do not update completed timestamp, this is due to slow updates of the consumed feeds.
        if (toUpdate.isEmpty()) {
            return null;
        }
        repository.updateLastScanComplete(rssSubscriptionDTO.getId());
        return toUpdate;
    }
}
