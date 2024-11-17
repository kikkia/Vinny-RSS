package com.rss.batch.processors;


import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RedditResponse;
import com.rss.model.RssProvider;
import com.rss.model.RssUpdate;
import com.rss.service.MetricsService;
import com.rss.utils.DislogLogger;
import com.rss.clients.HttpClient;
import com.rss.utils.RssUtils;
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
    private HttpClient client;
    private MetricsService metricsService;

    private String redditLOID;

    public RedditRssProcessor(RssSubscriptionRepository repository, HttpClient client, MetricsService service) {
        this.repository = repository;
        this.client = client;
        this.redditLOID = "";
        this.metricsService = service;
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        String url = RssUtils.Companion.getRedditUrl(rssSubscriptionDTO.getUrl());
        Instant lastScan = rssSubscriptionDTO.getLastScanComplete();
        //logger.info("Scanning subreddit " + rssSubscriptionDTO.getUrl());
        //logger.info("Using loid: " + redditLOID);
        ArrayList<JSONObject> toUpdate = new ArrayList<>();
        // TODO: Add support to scan rss endpoint to since that has a separate rate limit to double throughput
        try {
            RedditResponse response = client.getRedditJsonResponse(url, redditLOID);
            if (response.getJson().has("reason") && Objects.equals(response.getJson().getString("reason"), "banned") ) {
                logger.info("Subreddit " + rssSubscriptionDTO.getUrl() + "has been banned, removing");
                // Remove banned sub from scrape list
                repository.delete(rssSubscriptionDTO.getId());
                return Collections.emptyList();
            }
            if (!response.getJson().has("data")) {
                logger.warn("No data found in reddit response: " + response.getStatus());
                if (response.getStatus() == 429) {
                    logger.info("Too many requests, refreshing loid");
                    if (redditLOID.isEmpty()) {
                        redditLOID = response.getSessionTracker();
                    } else {
                        redditLOID = "";
                    }
                }
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
