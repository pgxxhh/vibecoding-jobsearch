package com.vibe.jobs.crawler.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "crawler.browser")
public class CrawlerBrowserProperties {

    private List<String> userAgents = List.of();
    private Map<String, String> extraHeaders = Map.of();
    private List<String> initScripts = List.of();
    private int viewportWidth = 1366;
    private int viewportHeight = 768;
    private Duration navigationTimeout = Duration.ofSeconds(90);
    private Duration defaultTimeout = Duration.ofSeconds(60);

    public List<String> getUserAgents() {
        return userAgents == null ? List.of() : Collections.unmodifiableList(userAgents);
    }

    public void setUserAgents(List<String> userAgents) {
        this.userAgents = userAgents;
    }

    public Map<String, String> getExtraHeaders() {
        return extraHeaders == null ? Map.of() : Collections.unmodifiableMap(extraHeaders);
    }

    public void setExtraHeaders(Map<String, String> extraHeaders) {
        this.extraHeaders = extraHeaders;
    }

    public List<String> getInitScripts() {
        return initScripts == null ? List.of() : Collections.unmodifiableList(initScripts);
    }

    public void setInitScripts(List<String> initScripts) {
        this.initScripts = initScripts;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(int viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(int viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    public Duration getNavigationTimeout() {
        return navigationTimeout == null ? Duration.ofSeconds(90) : navigationTimeout;
    }

    public void setNavigationTimeout(Duration navigationTimeout) {
        this.navigationTimeout = navigationTimeout;
    }

    public Duration getDefaultTimeout() {
        return defaultTimeout == null ? Duration.ofSeconds(60) : defaultTimeout;
    }

    public void setDefaultTimeout(Duration defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }
}
