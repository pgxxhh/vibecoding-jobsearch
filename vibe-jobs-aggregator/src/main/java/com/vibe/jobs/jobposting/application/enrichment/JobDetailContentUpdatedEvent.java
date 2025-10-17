package com.vibe.jobs.jobposting.application.enrichment;

public record JobDetailContentUpdatedEvent(
        Long jobDetailId,
        Long jobId,
        JobSnapshot job,
        String rawContent,
        String contentText,
        long contentVersion,
        String contentFingerprint
) {
}
