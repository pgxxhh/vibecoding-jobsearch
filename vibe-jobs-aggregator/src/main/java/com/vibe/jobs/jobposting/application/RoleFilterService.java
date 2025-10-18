package com.vibe.jobs.jobposting.application;

import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;
import com.vibe.jobs.ingestion.infrastructure.sourceclient.FetchedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleFilterService {

    private static final Logger log = LoggerFactory.getLogger(RoleFilterService.class);

    private final IngestionProperties properties;

    public RoleFilterService(IngestionProperties properties) {
        this.properties = properties;
    }

    public List<FetchedJob> filter(List<FetchedJob> jobs) {
        IngestionProperties.RoleFilter filter = properties.getRoleFilter();
        if (!filter.isEnabled() || jobs == null || jobs.isEmpty()) {
            return jobs;
        }

        List<FetchedJob> filtered = jobs.stream()
                .filter(job -> matches(filter, job))
                .collect(Collectors.toList());

        if (jobs.size() != filtered.size()) {
            log.info("Role filter: {} jobs -> {} jobs (filtered out {})", jobs.size(), filtered.size(), jobs.size() - filtered.size());
        }

        return filtered;
    }

    public String getFilterStatus() {
        IngestionProperties.RoleFilter filter = properties.getRoleFilter();
        if (!filter.isEnabled()) {
            return "Role filter: DISABLED";
        }
        StringBuilder status = new StringBuilder("Role filter: ENABLED\n");
        if (!filter.getIncludeKeywords().isEmpty()) {
            status.append("  Include keywords: ").append(filter.getIncludeKeywords()).append('\n');
        }
        if (!filter.getExcludeKeywords().isEmpty()) {
            status.append("  Exclude keywords: ").append(filter.getExcludeKeywords()).append('\n');
        }
        status.append("  Search description: ").append(filter.isSearchDescription());
        return status.toString().trim();
    }

    private boolean matches(IngestionProperties.RoleFilter filter, FetchedJob fetchedJob) {
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

    private boolean containsAny(List<String> keywords, String... haystacks) {
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

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String joinTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(" ", tags);
    }
}
