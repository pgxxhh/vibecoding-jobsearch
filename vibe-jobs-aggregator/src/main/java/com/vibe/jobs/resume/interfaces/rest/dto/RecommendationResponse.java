package com.vibe.jobs.resume.interfaces.rest.dto;

import java.util.List;

public record RecommendationResponse(List<RecommendationItemResponse> items) {
}
