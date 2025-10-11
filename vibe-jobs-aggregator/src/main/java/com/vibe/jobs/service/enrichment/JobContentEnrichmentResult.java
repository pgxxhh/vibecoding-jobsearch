package com.vibe.jobs.service.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.vibe.jobs.domain.JobEnrichmentKey;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record JobContentEnrichmentResult(
        boolean success,
        Map<JobEnrichmentKey, JsonNode> payload,
        String provider,
        Duration latency,
        String sourceFingerprint,
        List<String> warnings,
        JobContentEnrichmentResultError error
) {
    public JobContentEnrichmentResult {
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(payload));
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static JobContentEnrichmentResult success(Map<JobEnrichmentKey, JsonNode> payload,
                                                     String provider,
                                                     Duration latency,
                                                     String fingerprint,
                                                     List<String> warnings) {
        return new JobContentEnrichmentResult(true, payload, provider, latency, fingerprint, warnings, null);
    }

    public static JobContentEnrichmentResult failure(String provider,
                                                     String fingerprint,
                                                     String code,
                                                     String message) {
        return new JobContentEnrichmentResult(false, Map.of(), provider, Duration.ZERO, fingerprint, List.of(),
                new JobContentEnrichmentResultError(code, message));
    }

    public JobContentEnrichmentResult withFingerprint(String fingerprint) {
        if (Objects.equals(this.sourceFingerprint, fingerprint)) {
            return this;
        }
        return new JobContentEnrichmentResult(this.success, this.payload, this.provider, this.latency, fingerprint,
                this.warnings, this.error);
    }
}
