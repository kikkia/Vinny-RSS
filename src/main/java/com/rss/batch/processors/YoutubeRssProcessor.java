package com.rss.batch.processors;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.rss.clients.HttpClient;
import com.rss.config.properties.AuthProperties;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.HttpRequestException;
import com.rss.model.RssUpdate;
import com.rss.service.MetricsService;
import com.rss.utils.DislogLogger;
import com.rss.utils.RssUtils;
import org.json.JSONException;
import org.slf4j.MDC;
import org.springframework.batch.item.ItemProcessor;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class YoutubeRssProcessor implements ItemProcessor<RssSubscriptionDTO, List<RssUpdate>> {

    private final String LIVE_TAG = "\"key\":\"is_viewed_live\",\"value\":\"True\"";
    private RssSubscriptionRepository repository;
    private AuthProperties authProperties;
    private MetricsService metricsService;
    private DislogLogger logger = new DislogLogger(this.getClass());
    private HttpClient client;

    public YoutubeRssProcessor(RssSubscriptionRepository repository, HttpClient client, AuthProperties properties, MetricsService service) {
        this.repository = repository;
        this.client = client;
        this.authProperties = properties;
        this.metricsService = service;
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        try(MDC.MDCCloseable id = MDC.putCloseable("subscriptionId", rssSubscriptionDTO.getId() + "")) {
            String url = RssUtils.Companion.getYoutubeUrl(rssSubscriptionDTO.getUrl());
            Instant lastScan = rssSubscriptionDTO.getLastScanComplete();

            SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
            List<RssChannelSubscriptionDTO> subs = repository.getChannelSubsForSubcriptionId(rssSubscriptionDTO.getId());
            ArrayList<RssUpdate> toUpdate = new ArrayList<>();
            for (SyndEntry entry : feed.getEntries()) {
                Instant posted = entry.getPublishedDate().toInstant();
                if (posted.isAfter(lastScan)) {
                    boolean live = isLive(entry.getLink());
                    String subject = live ? "**VINNY**Live" + rssSubscriptionDTO.getUrl() : rssSubscriptionDTO.getUrl();
                    metricsService.markYTLiveExecute(rssSubscriptionDTO);
                    for (RssChannelSubscriptionDTO dto : subs) {
                        toUpdate.add(new RssUpdate(
                                rssSubscriptionDTO.getId(),
                                dto.getChannelId(),
                                entry.getLink(),
                                rssSubscriptionDTO.getProvider(),
                                subject,
                                entry.getAuthor()
                        ));
                    }
                }
            }
            // If empty do not update completed timestamp, this is due to slow updates of the consumed feeds.
            if (toUpdate.isEmpty()) {
                return null;
            }
            if (!repository.updateLastScanComplete(rssSubscriptionDTO.getId())) {
                logger.error("Failed to mark the last completed time, failing job");
                return null;
            }
            return toUpdate;
        } catch (Exception e) {
            logger.error("Failed to do YT rss scan", e);
            return null;
        }
    }

    private boolean isLive(String url) {
        try {
            return client.getJsonResponse(RssUtils.Companion.getYoutubeLiveUrl(url.split("/?v=")[1], authProperties.getYoutubeToken()))
                    .getJSONArray("items")
                    .getJSONObject(0)
                    .get("liveStreamingDetails") != null;
        } catch (HttpRequestException e) {
          logger.warn("Failed to get live for yt vid. code: " + e.getCode(), e);
          return false;
        } catch (JSONException e) {
            logger.warn("Failed to get live for yt vid", e);
            return false;
        }
    }
}
