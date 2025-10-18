
package com.vibe.jobs.jobposting.interfaces.rest.dto;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record JobDto(
        String id,
        String title,
        String company,
        String location,
        String level,
        Instant postedAt,
        List<String> tags,
        String url,
        Map<String, Object> enrichments,
        String summary,
        List<String> skills,
        List<String> highlights,
        boolean detailMatch
) {}
