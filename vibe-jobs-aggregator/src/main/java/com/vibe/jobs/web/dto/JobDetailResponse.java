package com.vibe.jobs.web.dto;

import java.time.Instant;

public record JobDetailResponse(
        Long id,
        String title,
        String company,
        String location,
        Instant postedAt,
        String content
) {}

