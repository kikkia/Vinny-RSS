package com.rss.batch;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssUpdate;
import com.rss.utils.RssUtils;
import org.springframework.batch.item.ItemProcessor;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TwitterRssProcessor implements ItemProcessor<RssSubscriptionDTO, List<RssUpdate>> {

    private RssSubscriptionRepository repository;

    public TwitterRssProcessor(RssSubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        String url = RssUtils.Companion.getTwitterUrl(rssSubscriptionDTO.getUrl());
        Instant lastScan = rssSubscriptionDTO.getLastScanComplete();

        SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
        List<RssChannelSubscriptionDTO> subs = repository.getChannelSubsForSubcriptionId(rssSubscriptionDTO.getId());
        ArrayList<RssUpdate> toUpdate = new ArrayList<>();
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
                            rt ? "RT@" + rssSubscriptionDTO.getUrl() : "@" + rssSubscriptionDTO.getUrl()
                            ));
                }
            }
        }
        repository.updateLastScanComplete(rssSubscriptionDTO.getId());
        return toUpdate;
    }
}
