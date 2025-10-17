package com.vibe.jobs.jobposting.application.dto;

import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public record JobDetailEnrichmentsDto(Long jobId,
                                      Map<JobEnrichmentKey, String> enrichmentJsonByKey) {

    public JobDetailEnrichmentsDto {
        Map<JobEnrichmentKey, String> value;
        if (enrichmentJsonByKey == null || enrichmentJsonByKey.isEmpty()) {
            value = Map.of();
        } else {
            value = new EnumMap<>(enrichmentJsonByKey);
        }
        enrichmentJsonByKey = Map.copyOf(value);
    }

    public Optional<String> findValue(JobEnrichmentKey key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(enrichmentJsonByKey.get(key));
    }
}
