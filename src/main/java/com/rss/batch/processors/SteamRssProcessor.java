package com.rss.batch.processors;

import com.rss.clients.HttpClient;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssProvider;
import com.rss.model.RssUpdate;
import com.rss.service.MetricsService;
import com.rss.utils.DislogLogger;
import com.rss.utils.RssUtils;
import org.json.JSONObject;
import org.slf4j.MDC;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SteamRssProcessor implements ItemProcessor<RssSubscriptionDTO, List<RssUpdate>> {

    private RssSubscriptionRepository repository;
    private MetricsService metricsService;
    private DislogLogger logger = new DislogLogger(this.getClass());
    private HttpClient client;

    public SteamRssProcessor(RssSubscriptionRepository repository, HttpClient client, MetricsService metricsService) {
        this.repository = repository;
        this.client = client;
        this.metricsService = metricsService;
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO dto) throws Exception {
        try(MDC.MDCCloseable id = MDC.putCloseable("subscriptionId", dto.getId() + "")) {
            String url = RssUtils.Companion.getSteamUrl(dto.getUrl());
            Instant lastScan = dto.getLastScanComplete();
            ArrayList<JSONObject> toUpdate = new ArrayList<>();


        } //catch (HttpRequestException e) {
            //logger.error("Failed to parse http response for steam rs Code: " + e.getCode(), e);
            //return null;
        //}
        catch (Exception e) {
            logger.error("Failed to get steam rss response", e);
            metricsService.markExecutionFailed(RssProvider.STEAM);
            return null;
        }
        return null;
    }
}
