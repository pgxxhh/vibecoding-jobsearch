package com.vibe.jobs.service.enrichment;

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
