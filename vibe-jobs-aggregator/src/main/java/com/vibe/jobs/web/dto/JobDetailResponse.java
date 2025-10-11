package com.vibe.jobs.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record JobDetailResponse(
        Long id,
        String title,
        String company,
        String location,
        Instant postedAt,
        String content,
        Map<String, Object> enrichments,
        Map<String, Object> enrichmentStatus,
        String summary,
        List<String> skills,
        List<String> highlights,
        String structuredData
) {}

