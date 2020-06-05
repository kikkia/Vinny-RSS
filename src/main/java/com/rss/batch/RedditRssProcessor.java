package com.rss.batch;

import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssUpdate;
import com.rss.utils.DislogLogger;
import org.springframework.batch.item.ItemProcessor;

public class RedditRssProcessor implements ItemProcessor<RssSubscriptionDTO, RssUpdate> {

    private DislogLogger logger = new DislogLogger(this.getClass());

    @Override
    public RssUpdate process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        // TODO
        return null;
    }
}
