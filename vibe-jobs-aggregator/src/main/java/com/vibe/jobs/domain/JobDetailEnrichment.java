package com.vibe.jobs.domain;

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
}
