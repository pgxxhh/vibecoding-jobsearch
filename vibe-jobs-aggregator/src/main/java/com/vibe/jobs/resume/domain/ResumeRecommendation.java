package com.vibe.jobs.resume.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ResumeRecommendation {
    private final Long jobId;
    private final String title;
    private final String company;
    private final String location;
    private final double score;
    private final List<String> skillHits;
    private final String explanation;
}
