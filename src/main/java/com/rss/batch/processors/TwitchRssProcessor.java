package com.rss.batch.processors;

import com.rss.clients.HttpClient;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssProvider;
import com.rss.model.RssUpdate;
import com.rss.service.MetricsService;
import com.rss.utils.DislogLogger;
import com.rss.utils.RssUtils;
import kotlin.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TwitchRssProcessor implements ItemProcessor<RssSubscriptionDTO, List<RssUpdate>> {

    private DislogLogger logger = new DislogLogger(this.getClass());
    private RssSubscriptionRepository repository;
    private HttpClient client;
    private MetricsService metricsService;
    private String clientId;
    private String clientSecret;
    private String oauthToken;
    private String refreshToken;
    private Long tokenExpiration;

    public TwitchRssProcessor(RssSubscriptionRepository repository, String clientId, String clientSecret, HttpClient client, MetricsService service) {
        this.clientId = clientId;
        this.client = client;
        this.repository = repository;
        this.clientSecret = clientSecret;
        this.metricsService = service;
        getOauthToken();
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        String url = RssUtils.Companion.getTwitchUrl(rssSubscriptionDTO.getUrl());
        Instant lastScan = rssSubscriptionDTO.getLastScanComplete();
        JSONObject toUpdate = null;
        try {
            if (System.currentTimeMillis() >= tokenExpiration) {
                getOauthToken();
            }
            List<Pair<String, String>> headers = Arrays.asList(new Pair<>("Client-ID", clientId),
                    new Pair<>("Authorization", "Bearer " + oauthToken),
                    new Pair<>("Accept", "application/vnd.twitchtv.v5+json"));
            JSONObject response = client.getJsonResponseWithHeaders(url, headers);
            JSONArray streams = response.getJSONArray("data");
            if (!streams.isEmpty()) {
                for (Object jsonObject : streams) {
                    if (jsonObject instanceof  JSONObject) {
                        JSONObject stream = (JSONObject) jsonObject;
                        if ((rssSubscriptionDTO.getUrl()).equals(stream.getString("user_id"))) {
                            // Update twitch displayname of the channel
                            if (!Objects.equals(rssSubscriptionDTO.getDisplayName(), stream.getString("user_name"))) {
                                String displayName = stream.getString("user_name");
                                rssSubscriptionDTO.setDisplayName(displayName);
                                repository.updateDisplayName(rssSubscriptionDTO.getId(),
                                        displayName);
                            }

                            Instant created = Instant.parse(stream.getString("started_at"));
                            if (created.isAfter(lastScan)) {
                                toUpdate = stream;
                                // We can break as twitch channels can only have one stream at a time
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch twitch feed for " + rssSubscriptionDTO.getUrl(), e);
            metricsService.markExecutionFailed(RssProvider.TWITCH);
            return null;
        }

        if (toUpdate == null) {
            return null;
        }

        ArrayList<RssUpdate> updates = new ArrayList<>();
        List<RssChannelSubscriptionDTO> subs = repository.getChannelSubsForSubcriptionId(rssSubscriptionDTO.getId());
        for (RssChannelSubscriptionDTO channel : subs) {
            updates.add(new RssUpdate(rssSubscriptionDTO.getId(),
                    channel.getChannelId(),
                    "https://twitch.tv/" + rssSubscriptionDTO.getDisplayName(),
                    rssSubscriptionDTO.getProvider(),
                    rssSubscriptionDTO.getUrl(),
                    toUpdate.getString("user_name")
            ));
        }
        // If empty do not update completed timestamp, this is due to slow updates of the consumed feeds.
        if (toUpdate.isEmpty()) {
            return null;
        }
        if (!repository.updateLastScanComplete(rssSubscriptionDTO.getId())) {
            logger.error("Failed to mark the last completed time, failing job");
            return null;
        }
        return updates.isEmpty() ? null : updates;
    }

    private void getOauthToken() {
        String url = "https://id.twitch.tv/oauth2/token?client_id=" + clientId + "&client_secret=" + clientSecret + "&grant_type=client_credentials";
        JSONObject response = client.postJsonResponse(url);
        oauthToken = response.getString("access_token");
        tokenExpiration = System.currentTimeMillis() + (response.getInt("expires_in") - 1) * 1000L;
    }
}
