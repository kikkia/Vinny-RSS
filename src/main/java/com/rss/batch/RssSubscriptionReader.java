package com.rss.batch;

import com.rss.db.model.RssSubscriptionDTO;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

public class RssSubscriptionReader implements ItemReader<RssSubscriptionDTO> {

    @Override
    public RssSubscriptionDTO read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        // TODO: Job context to pass provider
        return null;
    }
}
