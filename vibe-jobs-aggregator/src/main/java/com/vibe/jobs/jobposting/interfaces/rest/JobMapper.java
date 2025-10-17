
package com.vibe.jobs.jobposting.interfaces.rest;

import com.vibe.jobs.jobposting.application.dto.JobDetailEnrichmentsDto;
import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.jobposting.interfaces.rest.dto.JobDto;

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

    public static JobDto toDto(Job j, boolean detailMatch, JobDetailEnrichmentsDto enrichmentsDto) {
        List<String> tags = new ArrayList<>(j.getTags());
        
        JobEnrichmentExtractor.EnrichmentView enrichmentView = enrichmentsDto != null
                ? JobEnrichmentExtractor.extract(enrichmentsDto)
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
