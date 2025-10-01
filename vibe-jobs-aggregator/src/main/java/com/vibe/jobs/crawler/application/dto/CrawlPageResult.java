package com.vibe.jobs.crawler.application.dto;

import com.vibe.jobs.crawler.domain.CrawlMetrics;
import com.vibe.jobs.crawler.domain.CrawlResult;

import java.util.List;

public record CrawlPageResult(List<CrawlResult> results, CrawlMetrics metrics) {
    public CrawlPageResult {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
