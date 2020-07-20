package com.rss.batch;


import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssUpdate;
import com.rss.utils.DislogLogger;
import com.rss.clients.HttpClient;
import com.rss.utils.RssUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RedditRssProcessor implements ItemProcessor<RssSubscriptionDTO, List<RssUpdate>> {

    private DislogLogger logger = new DislogLogger(this.getClass());
    private RssSubscriptionRepository repository;
    private HttpClient client;

    public RedditRssProcessor(RssSubscriptionRepository repository, HttpClient client) {
        this.repository = repository;
        this.client = client;
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        String url = RssUtils.Companion.getRedditUrl(rssSubscriptionDTO.getUrl());
        Instant lastScan =  rssSubscriptionDTO.getLastScanComplete();

        ArrayList<JSONObject> toUpdate = new ArrayList<>();

        try {
            JSONObject response = client.getJsonResponse(url);
            JSONArray array = response.getJSONObject("data").getJSONArray("children");

            // Checking all posts in data
            for (Object jsonObject : array) {
                if (jsonObject instanceof  JSONObject) {
                    JSONObject post = (JSONObject) ((JSONObject) jsonObject).getJSONObject("data");
                    Instant posted = Instant.ofEpochSecond(post.getLong("created_utc"));
                    if (posted.isAfter(lastScan)) {
                        toUpdate.add(post);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse http response from reddit", e);
            return null;
        }

        ArrayList<RssUpdate> updates = new ArrayList<>();
        // Go over new posts and find the channels that they need to update
        List<RssChannelSubscriptionDTO> subs = repository.getChannelSubsForSubcriptionId(rssSubscriptionDTO.getId());

        for (JSONObject entry : toUpdate) {
            for (RssChannelSubscriptionDTO channel : subs) {
                String key = channel.getKeyword();
                if (key != null) {
                    if (entry.getString("title").contains(key)) {
                        updates.add(new RssUpdate(rssSubscriptionDTO.getId(),
                                channel.getChannelId(),
                                entry.getString("permalink"),
                                rssSubscriptionDTO.getProvider(),
                                entry.getString("subreddit_name_prefixed")));
                    }
                } else {
                    updates.add(new RssUpdate(rssSubscriptionDTO.getId(),
                            channel.getChannelId(),
                            entry.getString("permalink"),
                            rssSubscriptionDTO.getProvider(),
                            entry.getString("subreddit_name_prefixed")));
                }

            }
        }

        if (!repository.updateLastScanComplete(rssSubscriptionDTO.getId())) {
            logger.error("Failed to mark the last completed time, failing job");
            return null;
        }
        return updates.isEmpty() ? null : updates;
    }
}
