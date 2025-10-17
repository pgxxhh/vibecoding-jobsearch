package com.vibe.jobs.jobposting.application.enrichment;

import com.vibe.jobs.shared.infrastructure.config.JobDetailEnrichmentRetryProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JobDetailEnrichmentRetryStrategy {

    private final JobDetailEnrichmentRetryProperties properties;

    public JobDetailEnrichmentRetryStrategy(JobDetailEnrichmentRetryProperties properties) {
        this.properties = properties;
    }

    public Duration calculateDelay(int attemptNumber) {
        if (attemptNumber <= 0) {
            return Duration.ZERO;
        }
        Duration initial = properties.getInitialDelay();
        if (initial == null || initial.isNegative()) {
            return Duration.ZERO;
        }
        double multiplier = Math.max(1.0d, properties.getBackoffMultiplier());
        double factor = Math.pow(multiplier, Math.max(0, attemptNumber - 1));
        long initialMillis = Math.max(0L, initial.toMillis());
        long candidate = (long) Math.min(Long.MAX_VALUE, Math.round(initialMillis * factor));
        Duration maxDelay = properties.getMaxDelay();
        if (maxDelay != null && !maxDelay.isZero() && !maxDelay.isNegative()) {
            long maxMillis = maxDelay.toMillis();
            candidate = Math.min(candidate, maxMillis);
        }
        return Duration.ofMillis(candidate);
    }

    public int maxAttempts() {
        return Math.max(0, properties.getMaxAttempts());
    }

    public boolean retriesEnabled() {
        return properties.isEnabled() && maxAttempts() > 0;
    }

    public Duration inFlightGuard() {
        Duration guard = properties.getInFlightGuard();
        if (guard == null || guard.isNegative()) {
            return Duration.ZERO;
        }
        return guard;
    }

    public Duration schedulerInterval() {
        Duration interval = properties.getSchedulerInterval();
        if (interval == null || interval.isNegative()) {
            return Duration.ofMinutes(1);
        }
        return interval;
    }

    public int batchSize() {
        return Math.max(1, properties.getBatchSize());
    }
}
