package com.vibe.jobs.jobposting.infrastructure.persistence.entity;

import com.vibe.jobs.jobposting.domain.JobDetail;
import com.vibe.jobs.jobposting.domain.JobDetailEnrichment;
import com.vibe.jobs.jobposting.domain.JobDetailEnrichmentStatus;
import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "job_detail_enrichments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_detail_enrichments_job_key", columnNames = {"job_detail_id", "enrichment_key"})
})
@Where(clause = "deleted = 0")
public class JobDetailEnrichmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_detail_id", nullable = false)
    private JobDetailJpaEntity jobDetail;

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

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public JobDetailEnrichment toDomain(JobDetail detail) {
        JobDetailEnrichment enrichment = new JobDetailEnrichment(detail, enrichmentKey);
        enrichment.setId(id);
        enrichment.setValueJson(valueJson);
        enrichment.setSourceFingerprint(sourceFingerprint);
        enrichment.setProvider(provider);
        enrichment.setConfidence(confidence);
        enrichment.setMetadataJson(metadataJson);
        enrichment.setStatusState(statusState);
        enrichment.setRetryCount(retryCount);
        enrichment.setNextRetryAt(nextRetryAt);
        enrichment.setLastAttemptAt(lastAttemptAt);
        enrichment.setMaxAttempts(maxAttempts);
        enrichment.setCreatedAt(createdAt);
        enrichment.setUpdatedAt(updatedAt);
        enrichment.setDeleted(deleted);
        return enrichment;
    }

    public static JobDetailEnrichmentJpaEntity fromDomain(JobDetailEnrichment enrichment) {
        JobDetailEnrichmentJpaEntity entity = new JobDetailEnrichmentJpaEntity();
        entity.updateFromDomain(enrichment);
        return entity;
    }

    public void updateFromDomain(JobDetailEnrichment enrichment) {
        this.id = enrichment.getId();
        this.enrichmentKey = enrichment.getEnrichmentKey();
        this.valueJson = enrichment.getValueJson();
        this.sourceFingerprint = enrichment.getSourceFingerprint();
        this.provider = enrichment.getProvider();
        this.confidence = enrichment.getConfidence();
        this.metadataJson = enrichment.getMetadataJson();
        this.statusState = enrichment.getStatusState();
        this.retryCount = enrichment.getRetryCount();
        this.nextRetryAt = enrichment.getNextRetryAt();
        this.lastAttemptAt = enrichment.getLastAttemptAt();
        this.maxAttempts = enrichment.getMaxAttempts();
        this.createdAt = enrichment.getCreatedAt();
        this.updatedAt = enrichment.getUpdatedAt();
        this.deleted = enrichment.isDeleted();
    }

    public void syncDomain(JobDetailEnrichment enrichment) {
        enrichment.setId(id);
        enrichment.setEnrichmentKey(enrichmentKey);
        enrichment.setValueJson(valueJson);
        enrichment.setSourceFingerprint(sourceFingerprint);
        enrichment.setProvider(provider);
        enrichment.setConfidence(confidence);
        enrichment.setMetadataJson(metadataJson);
        enrichment.setStatusState(statusState);
        enrichment.setRetryCount(retryCount);
        enrichment.setNextRetryAt(nextRetryAt);
        enrichment.setLastAttemptAt(lastAttemptAt);
        enrichment.setMaxAttempts(maxAttempts);
        enrichment.setCreatedAt(createdAt);
        enrichment.setUpdatedAt(updatedAt);
        enrichment.setDeleted(deleted);
    }

    public JobEnrichmentKey getEnrichmentKey() {
        return enrichmentKey;
    }

    public Long getId() {
        return id;
    }

    public void setJobDetail(JobDetailJpaEntity jobDetail) {
        this.jobDetail = jobDetail;
    }

    public JobDetailJpaEntity getJobDetail() {
        return jobDetail;
    }

    public void markDeleted() {
        this.deleted = true;
        this.updatedAt = Instant.now();
    }

    public void markRetrying(Instant attemptedAt) {
        Instant now = Objects.requireNonNullElseGet(attemptedAt, Instant::now);
        this.statusState = JobDetailEnrichmentStatus.RETRYING;
        this.lastAttemptAt = now;
        this.nextRetryAt = null;
        this.updatedAt = now;
    }
}
