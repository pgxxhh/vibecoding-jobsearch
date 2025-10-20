package com.vibe.jobs.admin.web.dto;

import java.util.List;

public record CrawlerBlueprintGenerationRequest(
        String code,
        String name,
        String entryUrl,
        String searchKeywords,
        List<String> excludeSelectors,
        String notes
) {
}
