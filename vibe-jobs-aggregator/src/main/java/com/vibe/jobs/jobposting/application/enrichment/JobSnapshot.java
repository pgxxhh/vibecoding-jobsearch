package com.vibe.jobs.jobposting.application.enrichment;

import com.vibe.jobs.jobposting.domain.Job;

import java.util.List;
import java.util.Set;

public record JobSnapshot(
        Long id,
        String title,
        String company,
        String location,
        String level,
        String url,
        List<String> tags
) {

    public static JobSnapshot from(Job job) {
        if (job == null) {
            return new JobSnapshot(null, null, null, null, null, null, List.of());
        }
        Set<String> sourceTags = job.getTags();
        List<String> tags = sourceTags == null ? List.of() : List.copyOf(sourceTags);
        return new JobSnapshot(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getLevel(),
                job.getUrl(),
                tags
        );
    }
}
