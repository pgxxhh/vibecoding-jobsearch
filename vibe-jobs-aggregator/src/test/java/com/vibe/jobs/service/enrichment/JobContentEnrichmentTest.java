package com.vibe.jobs.service.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.domain.JobEnrichmentKey;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobContentEnrichmentTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void successResultShouldExposeImmutablePayload() {
        Map<JobEnrichmentKey, JsonNode> payload = new EnumMap<>(JobEnrichmentKey.class);
        payload.put(JobEnrichmentKey.SUMMARY, objectMapper.valueToTree("summary"));
        payload.put(JobEnrichmentKey.SKILLS, objectMapper.valueToTree(List.of("Java", "Spring")));

        JobContentEnrichmentResult result = JobContentEnrichmentResult.success(
                payload,
                "chatgpt",
                Duration.ofMillis(1200),
                "fingerprint",
                List.of("warning")
        );

        assertThat(result.success()).isTrue();
        assertThat(result.provider()).isEqualTo("chatgpt");
        assertThat(result.latency()).isEqualTo(Duration.ofMillis(1200));
        assertThat(result.sourceFingerprint()).isEqualTo("fingerprint");
        assertThat(result.warnings()).containsExactly("warning");
        assertThat(result.payload()).containsKeys(JobEnrichmentKey.SUMMARY, JobEnrichmentKey.SKILLS);

        assertThatThrownBy(() -> result.payload().put(JobEnrichmentKey.HIGHLIGHTS, objectMapper.createObjectNode()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void failureResultShouldContainError() {
        JobContentEnrichmentResult failure = JobContentEnrichmentResult.failure(
                "chatgpt",
                "abc",
                "TIMEOUT",
                "Timed out"
        );

        assertThat(failure.success()).isFalse();
        assertThat(failure.payload()).isEmpty();
        assertThat(failure.provider()).isEqualTo("chatgpt");
        assertThat(failure.sourceFingerprint()).isEqualTo("abc");
        assertThat(failure.error()).isNotNull();
        assertThat(failure.error().code()).isEqualTo("TIMEOUT");
        assertThat(failure.error().message()).isEqualTo("Timed out");
    }

    @Test
    void withFingerprintShouldReturnNewInstanceWhenDifferent() {
        JobContentEnrichmentResult result = JobContentEnrichmentResult.failure(
                "provider",
                "old",
                "ERROR",
                "oops"
        );

        JobContentEnrichmentResult updated = result.withFingerprint("new");
        assertThat(updated).isNotSameAs(result);
        assertThat(updated.sourceFingerprint()).isEqualTo("new");
        assertThat(updated.error()).isEqualTo(result.error());
    }

    @Test
    void withFingerprintShouldReturnSameInstanceWhenUnchanged() {
        JobContentEnrichmentResult result = JobContentEnrichmentResult.failure(
                "provider",
                "same",
                "ERROR",
                "oops"
        );

        assertThat(result.withFingerprint("same")).isSameAs(result);
    }
}
