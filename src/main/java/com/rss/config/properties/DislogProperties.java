package com.rss.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "dislog")
public class DislogProperties {
    private boolean enabled;
    private String hostIdentifier;
    private String username;
    private String avatarUrl;
    private List<String> debugWebhooks;
    private List<String> traceWebhooks;
    private List<String> infoWebhooks;
    private List<String> warnWebhooks;
    private List<String> errorWebhooks;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHostIdentifier() {
        return hostIdentifier;
    }

    public void setHostIdentifier(String hostIdentifier) {
        this.hostIdentifier = hostIdentifier;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public List<String> getDebugWebhooks() {
        return debugWebhooks;
    }

    public void setDebugWebhooks(List<String> debugWebhooks) {
        this.debugWebhooks = debugWebhooks;
    }

    public List<String> getTraceWebhooks() {
        return traceWebhooks;
    }

    public void setTraceWebhooks(List<String> traceWebhooks) {
        this.traceWebhooks = traceWebhooks;
    }

    public List<String> getInfoWebhooks() {
        return infoWebhooks;
    }

    public void setInfoWebhooks(List<String> infoWebhooks) {
        this.infoWebhooks = infoWebhooks;
    }

    public List<String> getWarnWebhooks() {
        return warnWebhooks;
    }

    public void setWarnWebhooks(List<String> warnWebhooks) {
        this.warnWebhooks = warnWebhooks;
    }

    public List<String> getErrorWebhooks() {
        return errorWebhooks;
    }

    public void setErrorWebhooks(List<String> errorWebhooks) {
        this.errorWebhooks = errorWebhooks;
    }
}