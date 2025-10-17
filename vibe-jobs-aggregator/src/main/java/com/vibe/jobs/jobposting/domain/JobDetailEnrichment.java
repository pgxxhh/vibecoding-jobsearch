package com.vibe.jobs.jobposting.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "job_detail_enrichments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_detail_enrichments_job_key", columnNames = {"job_detail_id", "enrichment_key"})
})
@Where(clause = "deleted = 0")
public class JobDetailEnrichment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_detail_id", nullable = false)
    private JobDetail jobDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "enrichment_key", nullable = false, length = 64)
    private JobEnrichmentKey enrichmentKey;

    @Lob
    @Column(name = "value_json", columnDefinition = "longtext")
    private String valueJson;

    @Column(name = "source_fingerprint", length = 128)
    private String sourceFingerprint;

    @Column(name = "provider", length = 128)
    private String provider;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Lob
    @Column(name = "metadata_json", columnDefinition = "longtext")
    private String metadataJson;

    @Column(name = "status_state", length = 32)
    private String statusState;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at", columnDefinition = "timestamp")
    private Instant nextRetryAt;

    @Column(name = "last_attempt_at", columnDefinition = "timestamp")
    private Instant lastAttemptAt;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp")
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    protected JobDetailEnrichment() {
    }

    public JobDetailEnrichment(JobDetail jobDetail, JobEnrichmentKey key) {
        this.jobDetail = jobDetail;
        this.enrichmentKey = key;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public JobDetail getJobDetail() {
        return jobDetail;
    }

    public JobEnrichmentKey getEnrichmentKey() {
        return enrichmentKey;
    }

    public String getValueJson() {
        return valueJson;
    }

    public String getSourceFingerprint() {
        return sourceFingerprint;
    }

    public String getProvider() {
        return provider;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getStatusState() {
        return statusState;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void markDeleted() {
        this.deleted = true;
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
            this.updatedAt = Instant.now();
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
