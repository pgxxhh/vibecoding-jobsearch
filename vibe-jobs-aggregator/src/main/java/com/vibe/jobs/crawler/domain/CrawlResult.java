package com.vibe.jobs.crawler.domain;

import com.vibe.jobs.ingestion.infrastructure.sourceclient.FetchedJob;

import java.util.Map;

public record CrawlResult(FetchedJob job, String rawContent, Map<String, Object> metadata) {
    public CrawlResult {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
