package com.vibe.jobs.crawler.domain;

import java.time.Instant;

public record CrawlRun(String id,
                        String blueprintCode,
                        String dataSourceCode,
                        String company,
                        int pageIndex,
                        int jobCount,
                        long durationMs,
                        boolean success,
                        String error,
                        Instant startedAt,
                        Instant completedAt) {
}
