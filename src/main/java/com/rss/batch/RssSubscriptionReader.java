package com.rss.batch;

import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssProvider;
import com.rss.utils.DislogLogger;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

public class RssSubscriptionReader implements ItemReader<RssSubscriptionDTO> {

    private DislogLogger logger = new DislogLogger(this.getClass());

    private RssSubscriptionRepository repository;
    private RssProvider provider;
    public RssSubscriptionReader(RssSubscriptionRepository repository, Long provider) {
        this.repository = repository;
        this.provider = RssProvider.Companion.getProvider(provider.intValue());
    }

    @Override
    public RssSubscriptionDTO read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        // TODO: Job context to pass provider
        RssSubscriptionDTO dto = repository.getNextSubscription(provider);
        logger.debug("Starting reddit sync for subreddit " + dto.getUrl());
        return dto;
    }
}
