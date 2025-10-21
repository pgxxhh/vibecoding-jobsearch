package com.vibe.jobs.admin.interfaces.dto;

import java.util.List;

public record CrawlerBlueprintDetailResponse(
        CrawlerBlueprintSummaryResponse summary,
        String draftConfig,
        Object lastTestReport,
        List<CrawlerBlueprintTaskResponse> recentTasks
) {
}
