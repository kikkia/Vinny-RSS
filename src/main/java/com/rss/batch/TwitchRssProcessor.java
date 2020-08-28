package com.rss.batch;

import com.rss.clients.HttpClient;
import com.rss.db.dao.RssSubscriptionRepository;
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

    public TwitchRssProcessor(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        String url = RssUtils.Companion.getTwitchUrl(rssSubscriptionDTO.getUrl());
        Instant lastScan = rssSubscriptionDTO.getLastScanComplete();
        ArrayList<JSONObject> toUpdate = new ArrayList<>();

        try {
            List<Pair<String, String>> headers = Arrays.asList(new Pair<>("Client-ID", clientId),
                    new Pair<>("Accept", "application/vnd.twitchtv.v5+json"));
            JSONObject response = client.getJsonResponseWithHeaders(url, headers);
            JSONArray videos = response.getJSONArray("videos");
            for (Object jsonObject : videos) {
                if (jsonObject instanceof  JSONObject) {
                    JSONObject video = (JSONObject) jsonObject;
                    if ("recording".equals(video.getString("status"))) {

                    }
                }
            }
        }
        return null;
    }
}
