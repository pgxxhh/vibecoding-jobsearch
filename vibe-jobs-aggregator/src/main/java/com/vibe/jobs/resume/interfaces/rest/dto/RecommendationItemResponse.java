package com.vibe.jobs.resume.interfaces.rest.dto;

import java.util.List;

public record RecommendationItemResponse(
        Long jobId,
        String title,
        String company,
        String location,
        double score,
        List<String> skillHits,
        String explanation
) {
}
