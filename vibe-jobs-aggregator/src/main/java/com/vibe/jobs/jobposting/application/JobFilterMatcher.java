package com.vibe.jobs.jobposting.application;

import com.vibe.jobs.ingestion.infrastructure.sourceclient.FetchedJob;
import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class JobFilterMatcher {

    private JobFilterMatcher() {
    }

    public static boolean matchesLocation(String location, IngestionProperties.LocationFilter filter) {
        if (filter == null) {
            return true;
        }
        return filter.matches(location);
    }

    public static boolean matchesRole(FetchedJob fetchedJob, IngestionProperties.RoleFilter filter) {
        if (filter == null || !filter.isEnabled()) {
            return true;
        }
        if (fetchedJob == null || fetchedJob.job() == null) {
            return false;
        }
        String title = normalize(fetchedJob.job().getTitle());
        String rawTitle = safe(fetchedJob.job().getTitle());
        String content = filter.isSearchDescription() ? normalize(fetchedJob.content()) : "";
        String rawContent = filter.isSearchDescription() ? safe(fetchedJob.content()) : "";
        String tags = normalize(joinTags(fetchedJob.job().getTags()));

        if (containsAny(filter.getExcludeKeywords(), title, rawTitle, content, rawContent, tags)) {
            return false;
        }

        if (filter.getIncludeKeywords().isEmpty()) {
            return true;
        }

        return containsAny(filter.getIncludeKeywords(), title, rawTitle, content, rawContent, tags);
    }

    private static boolean containsAny(List<String> keywords, String... haystacks) {
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT).trim();
            for (String haystack : haystacks) {
                if (haystack == null || haystack.isBlank()) {
                    continue;
                }
                if (haystack.contains(normalizedKeyword) || haystack.contains(keyword.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String joinTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(" ", tags);
    }
}
