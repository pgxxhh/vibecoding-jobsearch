package com.vibe.jobs.admin.web.dto;

public record CrawlerBlueprintActivationResponse(
        CrawlerBlueprintSummaryResponse blueprint,
        DataSourceResponse dataSource
) {
    public static CrawlerBlueprintActivationResponse of(CrawlerBlueprintSummaryResponse blueprint,
                                                        DataSourceResponse dataSource) {
        return new CrawlerBlueprintActivationResponse(blueprint, dataSource);
    }
}
