package com.rss.batch;

import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssProvider;
import com.rss.utils.DislogLogger;
import org.springframework.batch.item.ItemReader;

public class RssSubscriptionReader implements ItemReader<RssSubscriptionDTO> {

    private DislogLogger logger = new DislogLogger(this.getClass());

    private RssSubscriptionRepository repository;
    private RssProvider provider;

    public RssSubscriptionReader(RssSubscriptionRepository repository, RssProvider provider) {
        this.repository = repository;
        this.provider = provider;
    }

    @Override
    public RssSubscriptionDTO read() {
        RssSubscriptionDTO dto = repository.getNextSubscription(provider);
        if (dto == null) {
            logger.warn("No subreddit found for sync");
        } else {
            logger.debug("Starting reddit sync for subreddit " + dto.getUrl());
        }
        return dto;
    }
}
