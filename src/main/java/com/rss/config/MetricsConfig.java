package com.rss.config;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public StatsDClient dataDogStatsDClient(@Value("${metrics.hostname}") String hostname,
                                            @Value("&{metrics.prefix}") String prefix) {
        return new NonBlockingStatsDClient(
                prefix,
                hostname,
                8125,
                "vinny:rss");
    }
}
