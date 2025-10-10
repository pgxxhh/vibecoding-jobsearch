package com.vibe.jobs.web.dto;

import java.time.Instant;
import java.util.List;

public record JobDetailResponse(
        Long id,
        String title,
        String company,
        String location,
        Instant postedAt,
        String content,
        String summary,
        List<String> skills,
        List<String> highlights,
        String structuredData
) {}

