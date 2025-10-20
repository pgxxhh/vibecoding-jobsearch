package com.vibe.jobs.crawler.domain;

public enum CrawlerBlueprintStatus {
    DRAFT,
    READY,
    ACTIVE,
    FAILED;

    public static CrawlerBlueprintStatus from(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return CrawlerBlueprintStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ACTIVE;
        }
    }
}
