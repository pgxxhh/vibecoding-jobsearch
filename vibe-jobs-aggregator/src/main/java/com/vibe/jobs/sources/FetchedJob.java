package com.vibe.jobs.sources;

import com.vibe.jobs.domain.Job;

public record FetchedJob(Job job, String content) {
    public static FetchedJob of(Job job, String content) {
        return new FetchedJob(job, content);
    }
}

