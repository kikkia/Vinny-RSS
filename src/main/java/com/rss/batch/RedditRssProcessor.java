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
import org.springframework.batch.item.ItemProcessor;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RedditRssProcessor implements ItemProcessor<RssSubscriptionDTO, List<RssUpdate>> {

    private DislogLogger logger = new DislogLogger(this.getClass());
    private RssSubscriptionRepository repository;

    public RedditRssProcessor(RssSubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        // TODO
        String url = rssSubscriptionDTO.getUrl();
        Instant lastScan =  rssSubscriptionDTO.getLastScanComplete();

        ArrayList<SyndEntry> toUpdate = new ArrayList();

        SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
        System.out.println(feed.getTitle());
        for (SyndEntry e : feed.getEntries()) {
            System.out.println(e);
            Instant posted = e.getPublishedDate().toInstant();
            if (posted.isAfter(lastScan)) {
                toUpdate.add(e);
            }
        }

        ArrayList<RssUpdate> updates = new ArrayList<>();
        // Go over new posts and find the channels that they need to update
        List<RssChannelSubscriptionDTO> subs = repository.getChannelSubsForSubcriptionId(rssSubscriptionDTO.getId());

        for (SyndEntry entry : toUpdate) {
            List<String> channelIds = new ArrayList<>();
            for (RssChannelSubscriptionDTO channel : subs) {
                String key = channel.getKeyword();
                if (key != null) {
                    if (entry.getTitle().contains(key)
                            || entry.getLink().contains(key)
                            || entry.getDescription().getValue().contains(key)) {
                        channelIds.add(channel.getChannelId());
                    }
                } else {
                    channelIds.add(channel.getChannelId());
                }
            }
            updates.add(new RssUpdate(rssSubscriptionDTO.getId(), channelIds, entry.getLink()));
        }
        return updates.isEmpty() ? null : updates;
    }
}
