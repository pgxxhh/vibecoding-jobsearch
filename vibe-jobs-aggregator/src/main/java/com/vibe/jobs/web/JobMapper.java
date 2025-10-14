
package com.vibe.jobs.web;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.web.dto.JobDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class JobMapper {
    public static JobDto toDto(Job j) {
        return toDto(j, false, null);
    }

    public static JobDto toDto(Job j, boolean detailMatch) {
        return toDto(j, detailMatch, null);
    }

    public static JobDto toDto(Job j, boolean detailMatch, JobDetail detail) {
        List<String> tags = new ArrayList<>(j.getTags());
        
        JobEnrichmentExtractor.EnrichmentView enrichmentView = detail != null
                ? JobEnrichmentExtractor.extract(detail)
                : JobEnrichmentExtractor.EnrichmentView.empty();

        String summary = enrichmentView.summary().orElse(null);
        List<String> skills = sanitizeList(enrichmentView.skills());
        List<String> highlights = sanitizeList(enrichmentView.highlights());
        Map<String, Object> enrichments = enrichmentView.enrichments();

        return new JobDto(
                String.valueOf(j.getId()),
                j.getTitle(),
                j.getCompany(),
                j.getLocation(),
                j.getLevel(),
                j.getPostedAt(),
                tags,
                j.getUrl(),
                enrichments,
                summary,
                skills,
                highlights,
                detailMatch
        );
    }

    private static List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }

}
