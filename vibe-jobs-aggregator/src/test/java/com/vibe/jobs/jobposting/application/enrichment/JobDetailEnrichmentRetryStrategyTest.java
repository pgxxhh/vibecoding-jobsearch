package com.vibe.jobs.jobposting.application.enrichment;

import com.vibe.jobs.shared.infrastructure.config.JobDetailEnrichmentRetryProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JobDetailEnrichmentRetryStrategyTest {

    @Test
    void calculateDelayRespectsBackoffAndMax() {
        JobDetailEnrichmentRetryProperties properties = new JobDetailEnrichmentRetryProperties();
        properties.setInitialDelay(Duration.ofSeconds(30));
        properties.setBackoffMultiplier(2.0d);
        properties.setMaxDelay(Duration.ofMinutes(2));
        properties.setMaxAttempts(5);
        JobDetailEnrichmentRetryStrategy strategy = new JobDetailEnrichmentRetryStrategy(properties);

        assertThat(strategy.calculateDelay(1)).isEqualTo(Duration.ofSeconds(30));
        assertThat(strategy.calculateDelay(2)).isEqualTo(Duration.ofSeconds(60));
        assertThat(strategy.calculateDelay(3)).isEqualTo(Duration.ofSeconds(120));
        assertThat(strategy.calculateDelay(4)).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void retriesDisabledWhenFlagFalse() {
        JobDetailEnrichmentRetryProperties properties = new JobDetailEnrichmentRetryProperties();
        properties.setEnabled(false);
        properties.setMaxAttempts(3);
        JobDetailEnrichmentRetryStrategy strategy = new JobDetailEnrichmentRetryStrategy(properties);

        assertThat(strategy.retriesEnabled()).isFalse();
        assertThat(strategy.maxAttempts()).isEqualTo(3);
    }
}
