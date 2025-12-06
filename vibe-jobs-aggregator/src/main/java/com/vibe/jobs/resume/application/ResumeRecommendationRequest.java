package com.vibe.jobs.resume.application;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResumeRecommendationRequest {
    private final Long resumeId;
    private final String location;
    private final Integer limit;
    private final Integer experienceYears;
    private final Integer minSalary;
    private final Boolean remoteOnly;
}
