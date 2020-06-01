package com.rss.config;

import com.kikkia.dislog.api.DislogClient;
import com.kikkia.dislog.models.LogLevel;
import com.rss.config.properties.DislogProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DislogConfig {

    @Bean
    public DislogClient client(DislogProperties properties) {
        if (!properties.isEnabled())
            return null;

        DislogClient.Builder builder = new DislogClient.Builder()
                .setUsername(properties.getUsername())
                .setIdentifier(properties.getHostIdentifier())
                .setAvatarUrl(properties.getAvatarUrl());

        HashMap<LogLevel, List<String>> hookMap = new HashMap<>();
        hookMap.put(LogLevel.DEBUG, properties.getDebugWebhooks());
        hookMap.put(LogLevel.TRACE, properties.getTraceWebhooks());
        hookMap.put(LogLevel.INFO, properties.getInfoWebhooks());
        hookMap.put(LogLevel.WARN, properties.getWarnWebhooks());
        hookMap.put(LogLevel.ERROR, properties.getErrorWebhooks());

        for (Map.Entry<LogLevel, List<String>> list : hookMap.entrySet()) {
            for (String url : list.getValue()) {
                builder.addWebhook(list.getKey(), url);
            }
        }

        return builder.build();
    }
}