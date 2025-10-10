
package com.vibe.jobs.web;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.web.dto.JobDto;

import java.util.ArrayList;
import java.util.List;
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
        String summary = detail != null ? trimToNull(detail.getSummary()) : null;
        List<String> skills = detail != null ? sanitizeList(detail.getSkills()) : List.of();
        List<String> highlights = detail != null ? sanitizeList(detail.getHighlights()) : List.of();

        return new JobDto(
                String.valueOf(j.getId()),
                j.getTitle(),
                j.getCompany(),
                j.getLocation(),
                j.getLevel(),
                j.getPostedAt(),
                tags,
                j.getUrl(),
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

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
