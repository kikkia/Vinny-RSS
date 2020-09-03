package com.rss.service;

import com.rss.model.RssProvider;
import com.timgroup.statsd.StatsDClient;
import org.springframework.stereotype.Component;

@Component
public class MetricsService {

    private StatsDClient client;

    public MetricsService(StatsDClient client) {
        this.client = client;
    }

    public void markReadExectution(RssProvider provider, long timeSinceLastAttempt, long timeSinceLastCompleted) {
        String providerTag = "provider:" + provider.name();
        client.incrementCounter("rss.update.updated", providerTag);
        client.time("rss.update.attemptLatency", timeSinceLastAttempt, providerTag);
        client.time("rss.update.completedLatency", timeSinceLastCompleted, providerTag);
    }
}
