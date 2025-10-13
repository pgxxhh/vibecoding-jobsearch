package com.vibe.jobs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jobs.detail-enhancement.retry")
public class JobDetailEnrichmentRetryProperties {

    private boolean enabled = true;
    private int maxAttempts = 5;
    private Duration initialDelay = Duration.ofMinutes(1);
    private double backoffMultiplier = 2.0d;
    private Duration maxDelay = Duration.ofMinutes(30);
    private Duration schedulerInterval = Duration.ofMinutes(1);
    private int batchSize = 20;
    private Duration inFlightGuard = Duration.ofMinutes(5);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(Duration initialDelay) {
        this.initialDelay = initialDelay;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(Duration maxDelay) {
        this.maxDelay = maxDelay;
    }

    public Duration getSchedulerInterval() {
        return schedulerInterval;
    }

    public void setSchedulerInterval(Duration schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Duration getInFlightGuard() {
        return inFlightGuard;
    }

    public void setInFlightGuard(Duration inFlightGuard) {
        this.inFlightGuard = inFlightGuard;
    }
}
