package com.vibe.jobs.ingestion.infrastructure.sourceclient;

import com.vibe.jobs.jobposting.domain.Job;

public record FetchedJob(Job job, String content) {
    public static FetchedJob of(Job job, String content) {
        return new FetchedJob(job, content);
    }
}

