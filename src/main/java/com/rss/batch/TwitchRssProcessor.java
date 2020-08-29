package com.rss.batch;

import com.rss.clients.HttpClient;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssUpdate;
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

public class TwitchRssProcessor implements ItemProcessor<RssSubscriptionDTO, List<RssUpdate>> {

    private DislogLogger logger = new DislogLogger(this.getClass());
    private RssSubscriptionRepository repository;
    private HttpClient client;
    private String clientId;

    public TwitchRssProcessor(RssSubscriptionRepository repository, String clientId, HttpClient client) {
        this.clientId = clientId;
        this.client = client;
        this.repository = repository;
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        String url = RssUtils.Companion.getTwitchUrl(rssSubscriptionDTO.getUrl());
        Instant lastScan = rssSubscriptionDTO.getLastScanComplete();
        JSONObject toUpdate = null;
        try {
            List<Pair<String, String>> headers = Arrays.asList(new Pair<>("Client-ID", clientId),
                    new Pair<>("Accept", "application/vnd.twitchtv.v5+json"));
            JSONObject response = client.getJsonResponseWithHeaders(url, headers);
            JSONArray videos = response.getJSONArray("videos");
            for (Object jsonObject : videos) {
                if (jsonObject instanceof  JSONObject) {
                    JSONObject video = (JSONObject) jsonObject;
                    if ("recording".equals(video.getString("status"))) {
                        Instant created = Instant.parse(video.getString("created_at"));
                        if (created.isAfter(lastScan)) {
                            toUpdate = video;
                            // We can break as twitch channels can only have one stream at a time
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch twitch feed for " + rssSubscriptionDTO.getUrl(), e);
            e.printStackTrace();
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
                    toUpdate.getJSONObject("channel").getString("url"),
                    rssSubscriptionDTO.getProvider(),
                    rssSubscriptionDTO.getUrl(),
                    toUpdate.getJSONObject("channel").getString("display_name")
            ));
        }

        if (!repository.updateLastScanComplete(rssSubscriptionDTO.getId())) {
            logger.error("Failed to mark the last completed time, failing job");
            return null;
        }
        return updates.isEmpty() ? null : updates;
    }
}
