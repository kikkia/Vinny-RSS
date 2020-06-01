package com.rss.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    private List<String> clientIps;
    private String clientToken;

    public List<String> getClientIps() {
        return clientIps;
    }

    public void setClientIps(List<String> clientIps) {
        this.clientIps = clientIps;
    }

    public String getClientToken() {
        return clientToken;
    }

    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }
}
