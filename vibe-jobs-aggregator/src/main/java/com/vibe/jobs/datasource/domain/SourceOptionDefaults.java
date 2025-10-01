package com.vibe.jobs.datasource.domain;

import java.util.Locale;
import java.util.Map;

public final class SourceOptionDefaults {

    private SourceOptionDefaults() {
    }

    public static Map<String, String> derive(String type, PlaceholderContext context) {
        if (type == null || type.isBlank()) {
            return Map.of();
        }
        String normalizedType = type.toLowerCase(Locale.ROOT);
        String slug = context.slug();
        String company = context.company();
        return switch (normalizedType) {
            case "greenhouse" -> Map.of("slug", slug);
            case "lever" -> Map.of("company", slug);
            case "workday" -> Map.of(
                    "baseUrl", slug.isBlank() ? "" : "https://" + slug + ".wd1.myworkdayjobs.com",
                    "tenant", slug,
                    "site", context.slugUpper()
            );
            case "ashby" -> Map.of(
                    "company", company,
                    "baseUrl", slug.isBlank() ? "" : "https://jobs.ashbyhq.com/" + slug
            );
            case "smartrecruiters" -> Map.of("company", company);
            case "recruitee" -> Map.of("company", slug);
            case "workable" -> Map.of("company", slug);
            default -> Map.of();
        };
    }
}
