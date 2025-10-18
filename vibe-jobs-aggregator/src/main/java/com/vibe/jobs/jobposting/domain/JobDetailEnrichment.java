package com.vibe.jobs.jobposting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class JobDetailEnrichment {

    private Long id;
    private JobDetail jobDetail;
    private JobEnrichmentKey enrichmentKey;
    private String valueJson;
    private String sourceFingerprint;
    private String provider;
    private BigDecimal confidence;
    private String metadataJson;
    private String statusState;
    private int retryCount;
    private Instant nextRetryAt;
    private Instant lastAttemptAt;
    private Integer maxAttempts;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;

    public JobDetailEnrichment() {
    }

    public JobDetailEnrichment(JobDetail jobDetail, JobEnrichmentKey key) {
        this.jobDetail = jobDetail;
        this.enrichmentKey = key;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JobDetail getJobDetail() {
        return jobDetail;
    }

    public void setJobDetail(JobDetail jobDetail) {
        this.jobDetail = jobDetail;
    }

    public JobEnrichmentKey getEnrichmentKey() {
        return enrichmentKey;
    }

    public void setEnrichmentKey(JobEnrichmentKey enrichmentKey) {
        this.enrichmentKey = enrichmentKey;
    }

    public String getValueJson() {
        return valueJson;
    }

    public void setValueJson(String valueJson) {
        this.valueJson = valueJson;
    }

    public String getSourceFingerprint() {
        return sourceFingerprint;
    }

    public void setSourceFingerprint(String sourceFingerprint) {
        this.sourceFingerprint = sourceFingerprint;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getStatusState() {
        return statusState;
    }

    public void setStatusState(String statusState) {
        this.statusState = statusState;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(Instant lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void markDeleted() {
        this.deleted = true;
    }

    public void markCreated(Instant timestamp) {
        Instant safeTimestamp = timestamp != null ? timestamp : Instant.now();
        this.createdAt = safeTimestamp;
        this.updatedAt = safeTimestamp;
    }

    public void markUpdated(Instant timestamp) {
        this.updatedAt = timestamp != null ? timestamp : Instant.now();
    }

    public void updateValue(String valueJson, String provider, String fingerprint, BigDecimal confidence, String metadataJson) {
        boolean changed = false;
        if (!Objects.equals(this.valueJson, valueJson)) {
            this.valueJson = valueJson;
            changed = true;
        }
        if (!Objects.equals(this.provider, provider)) {
            this.provider = provider;
            changed = true;
        }
        if (!Objects.equals(this.sourceFingerprint, fingerprint)) {
            this.sourceFingerprint = fingerprint;
            changed = true;
        }
        if (!Objects.equals(this.confidence, confidence)) {
            this.confidence = confidence;
            changed = true;
        }
        if (!Objects.equals(this.metadataJson, metadataJson)) {
            this.metadataJson = metadataJson;
            changed = true;
        }
        if (changed) {
            markUpdated(null);
        }
    }

    public void markSucceeded(int configuredMaxAttempts, Instant attemptTime) {
        Instant effectiveTime = attemptTime != null ? attemptTime : Instant.now();
        this.statusState = JobDetailEnrichmentStatus.SUCCESS;
        this.retryCount = 0;
        this.nextRetryAt = null;
        this.lastAttemptAt = effectiveTime;
        this.maxAttempts = configuredMaxAttempts;
        this.updatedAt = effectiveTime;
    }

    public void markRetryScheduled(int newRetryCount, Instant nextAttempt, int configuredMaxAttempts, Instant attemptTime) {
        Instant effectiveTime = attemptTime != null ? attemptTime : Instant.now();
        this.statusState = JobDetailEnrichmentStatus.RETRY_SCHEDULED;
        this.retryCount = newRetryCount;
        this.nextRetryAt = nextAttempt;
        this.lastAttemptAt = effectiveTime;
        this.maxAttempts = configuredMaxAttempts;
        this.updatedAt = effectiveTime;
    }

    public void markRetrying(Instant attemptTime) {
        Instant effectiveTime = attemptTime != null ? attemptTime : Instant.now();
        this.statusState = JobDetailEnrichmentStatus.RETRYING;
        this.lastAttemptAt = effectiveTime;
        this.nextRetryAt = null;
        this.updatedAt = effectiveTime;
    }

    public void markFailedTerminal(int configuredMaxAttempts, int retryCount, Instant attemptTime) {
        Instant effectiveTime = attemptTime != null ? attemptTime : Instant.now();
        this.statusState = JobDetailEnrichmentStatus.FAILED;
        this.nextRetryAt = null;
        this.retryCount = retryCount;
        this.maxAttempts = configuredMaxAttempts;
        this.lastAttemptAt = effectiveTime;
        this.updatedAt = effectiveTime;
    }
}
