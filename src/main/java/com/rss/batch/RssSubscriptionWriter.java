package com.rss.batch;

import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.model.RssUpdate;
import org.springframework.batch.item.ItemWriter;

import java.util.Arrays;
import java.util.List;

public class RssSubscriptionWriter implements ItemWriter<List<RssUpdate>> {

    @Override
    public void write(List<? extends List<RssUpdate>> list) throws Exception {
        // TODO: Post these updates to the main vinny api (which is not done yet)
        // for now just log em
        System.out.println(Arrays.toString(list.toArray()));
    }
}
