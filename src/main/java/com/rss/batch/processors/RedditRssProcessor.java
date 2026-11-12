package com.rss.batch.processors;


import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RedditResponse;
import com.rss.model.RssProvider;
import com.rss.model.RssUpdate;
import com.rss.service.MetricsService;
import com.rss.utils.DislogLogger;
import com.rss.clients.RedditClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RedditRssProcessor implements ItemProcessor<RssSubscriptionDTO, List<RssUpdate>> {

    private DislogLogger logger = new DislogLogger(this.getClass());
    private RssSubscriptionRepository repository;
    private RedditClient client;
    private MetricsService metricsService;

    public RedditRssProcessor(RssSubscriptionRepository repository, RedditClient client, MetricsService service) {
        this.repository = repository;
        this.client = client;
        this.metricsService = service;
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        Instant lastScan = rssSubscriptionDTO.getLastScanComplete();
        ArrayList<JSONObject> toUpdate = new ArrayList<>();
        try {
            RedditResponse response = client.getNewPosts(rssSubscriptionDTO.getUrl());
            if (response.getJson().has("reason") && Objects.equals(response.getJson().getString("reason"), "banned") ) {
                logger.info("Subreddit " + rssSubscriptionDTO.getUrl() + "has been banned, removing");
                // Remove banned sub from scrape list
                repository.delete(rssSubscriptionDTO.getId());
                return Collections.emptyList();
            }
            if (!response.getJson().has("data")) {
                logger.warn("No data found in reddit response: " + response.getStatus());
                if (response.getStatus() == 404) {
                    logger.info("Subreddit " + rssSubscriptionDTO.getUrl() + "has been banned, removing");
                    // Remove banned sub from scrape list
                    repository.delete(rssSubscriptionDTO.getId());
                }
                return Collections.emptyList();
            }
            JSONArray array = response.getJson().getJSONObject("data").getJSONArray("children");

            // Checking all posts in data
            for (Object jsonObject : array) {
                if (jsonObject instanceof  JSONObject) {
                    JSONObject post = ((JSONObject) jsonObject).getJSONObject("data");
                    Instant posted = Instant.ofEpochSecond(post.getLong("created_utc"));
                    // Is after last scan and less than 2 hours old
                    if (posted.isAfter(lastScan) && Instant.now().minusSeconds(posted.getEpochSecond()).getEpochSecond() < 7200) {
                        toUpdate.add(post);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse http response from reddit: " + rssSubscriptionDTO.getUrl(), e);
            // Throw to prevent spam of more retries
            metricsService.markExecutionFailed(RssProvider.REDDIT);
            throw e;
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
                                rssSubscriptionDTO.getUrl(),
                                entry.getString("subreddit_name_prefixed")));
                    }
                } else {
                    updates.add(new RssUpdate(rssSubscriptionDTO.getId(),
                            channel.getChannelId(),
                            entry.getString("permalink"),
                            rssSubscriptionDTO.getProvider(),
                            rssSubscriptionDTO.getUrl(),
                            entry.getString("subreddit_name_prefixed")));
                }

            }
        }
        // If empty do not update completed timestamp, this is due to slow updates of the consumed feeds.
        if (toUpdate.isEmpty()) {
            return Collections.emptyList();
        }
        if (!repository.updateLastScanComplete(rssSubscriptionDTO.getId())) {
            logger.error("Failed to mark the last completed time, failing job");
            return Collections.emptyList();
        }
        return updates;
    }
}
