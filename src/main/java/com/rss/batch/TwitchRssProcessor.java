package com.rss.batch;

import com.rss.clients.HttpClient;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssUpdate;
import com.rss.utils.DislogLogger;
import com.rss.utils.RssUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TwitchRssProcessor implements ItemProcessor<RssSubscriptionDTO, List<RssUpdate>> {

    private DislogLogger logger = new DislogLogger(this.getClass());
    private RssSubscriptionRepository repository;
    private HttpClient client;

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        String url = RssUtils.Companion.getTwitchUrl(rssSubscriptionDTO.getUrl());
        Instant lastScan = rssSubscriptionDTO.getLastScanComplete();
        ArrayList<JSONObject> toUpdate = new ArrayList<>();

        try {
            JSONObject response = client.getJsonResponse(url);
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
