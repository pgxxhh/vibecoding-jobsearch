package com.vibe.jobs.crawler.domain;

public record CrawlMetrics(long durationMs, int jobCount, int pageIndex, boolean success, String error) {
    public CrawlMetrics {
        error = error == null ? "" : error;
    }
}
