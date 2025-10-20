package com.vibe.jobs.admin.web.dto;

import com.vibe.jobs.crawler.domain.CrawlerBlueprintGenerationTask;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CrawlerBlueprintTaskResponse(
        Long id,
        String blueprintCode,
        CrawlerBlueprintGenerationTask.Status status,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage,
        Map<String, Object> browserSnapshot,
        List<Map<String, Object>> sampleData,
        Map<String, Object> inputPayload
) {
    public static CrawlerBlueprintTaskResponse fromDomain(CrawlerBlueprintGenerationTask task) {
        return new CrawlerBlueprintTaskResponse(
                task.id(),
                task.blueprintCode(),
                task.status(),
                task.startedAt().orElse(null),
                task.finishedAt().orElse(null),
                task.errorMessage(),
                task.browserSnapshot(),
                task.sampleData(),
                task.inputPayload()
        );
    }
}
