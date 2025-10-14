
package com.vibe.jobs.web;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.service.dto.JobDetailEnrichmentsDto;
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

    public static JobDto toDto(Job j, boolean detailMatch, JobDetailEnrichmentsDto enrichmentsDto) {
        List<String> tags = new ArrayList<>(j.getTags());

        // 使用JobEnrichmentExtractor来提取enrichment数据
        String summary = enrichmentsDto != null ? JobEnrichmentExtractor.summary(enrichmentsDto).orElse(null) : null;
        List<String> skills = enrichmentsDto != null ? sanitizeList(JobEnrichmentExtractor.skills(enrichmentsDto)) : List.of();
        List<String> highlights = enrichmentsDto != null ? sanitizeList(JobEnrichmentExtractor.highlights(enrichmentsDto)) : List.of();
        Map<String, Object> enrichments = enrichmentsDto != null ? JobEnrichmentExtractor.enrichments(enrichmentsDto) : Map.of();

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
