package com.vibe.jobs.crawler.infrastructure.engine;

import java.util.List;
import java.util.Map;

public record CrawlPageSnapshot(String pageContent,
                                List<String> detailContents,
                                Map<String, Object> metadata) {
    public CrawlPageSnapshot {
        detailContents = detailContents == null ? List.of() : List.copyOf(detailContents);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
