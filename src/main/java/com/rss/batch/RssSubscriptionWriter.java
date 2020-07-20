package com.rss.batch;

import com.rss.clients.MessagingClient;
import com.rss.model.RssUpdate;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class RssSubscriptionWriter implements ItemWriter<List<RssUpdate>> {

    private MessagingClient messagingClient;

    public RssSubscriptionWriter(MessagingClient messagingClient) {
        this.messagingClient = messagingClient;
    }
    @Override
    public void write(List<? extends List<RssUpdate>> list) throws Exception {
        // for now just log em
        for (List<RssUpdate> updateList : list) {
            for (RssUpdate update : updateList) {
                messagingClient.sendRssUpdate(update);
            }
        }
        System.out.println(list);
    }
}
