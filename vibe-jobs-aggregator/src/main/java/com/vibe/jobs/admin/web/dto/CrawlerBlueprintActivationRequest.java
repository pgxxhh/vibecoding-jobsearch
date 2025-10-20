package com.vibe.jobs.admin.web.dto;

public record CrawlerBlueprintActivationRequest(
        String dataSourceCode,
        Boolean enable
) {
    public boolean enableOrDefault() {
        return enable == null || Boolean.TRUE.equals(enable);
    }
}
