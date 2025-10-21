package com.vibe.jobs.admin.interfaces.dto;

public record CrawlerBlueprintActivationRequest(
        String dataSourceCode,
        Boolean enable
) {
    public boolean enableOrDefault() {
        return enable == null || Boolean.TRUE.equals(enable);
    }
}
