package com.vibe.jobs.service.enrichment;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JobContentEnrichmentResultTest {

    @Test
    void retryableWhenHttp429Failure() {
        JobContentEnrichmentResult result = JobContentEnrichmentResult.failure("deepseek", "fp", "HTTP_429", "Too many requests");
        assertThat(result.isRetryable()).isTrue();
    }

    @Test
    void nonRetryableForClientError() {
        JobContentEnrichmentResult result = JobContentEnrichmentResult.failure("deepseek", "fp", "HTTP_404", "not found");
        assertThat(result.isRetryable()).isFalse();
    }

    @Test
    void successIsNeverRetryable() {
        JobContentEnrichmentResult result = JobContentEnrichmentResult.success(Map.of(),
                "deepseek", Duration.ofMillis(10), "fp", java.util.List.of());
        assertThat(result.isRetryable()).isFalse();
    }
}
