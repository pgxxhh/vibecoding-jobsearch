package com.vibe.jobs.jobposting.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "job_details", indexes = {
        @Index(name = "idx_job_details_job_id", columnList = "job_id", unique = true),
        @Index(name = "idx_job_details_deleted", columnList = "deleted")
})
@Where(clause = "deleted = false")
public class JobDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    @Lob
    @Column(columnDefinition = "longtext")
    private String content;

    @Lob
    @Column(name = "content_text", columnDefinition = "longtext")
    private String contentText;

    @OneToMany(mappedBy = "jobDetail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Where(clause = "deleted = 0")
    private Set<JobDetailEnrichment> enrichments = new LinkedHashSet<>();

    @Column(nullable = false, columnDefinition = "timestamp")
    private Instant createdAt;

    @Column(nullable = false, columnDefinition = "timestamp")
    private Instant updatedAt;

    // 软删除字段
    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "content_version", nullable = false)
    private long contentVersion = 0L;

    protected JobDetail() {
    }

    public JobDetail(Job job, String content, String contentText) {
        this.job = job;
        this.content = content;
        this.contentText = contentText;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void delete() {
        this.deleted = true;
        this.updatedAt = Instant.now();
    }

    public boolean isNotDeleted() {
        return !deleted;
    }

    public Long getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
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

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Set<JobDetailEnrichment> getEnrichments() {
        return enrichments;
    }

    public Optional<JobDetailEnrichment> findEnrichment(JobEnrichmentKey key) {
        if (key == null || enrichments == null || enrichments.isEmpty()) {
            return Optional.empty();
        }
        return enrichments.stream()
                .filter(enrichment -> enrichment.getEnrichmentKey() == key)
                .findFirst();
    }

    public JobDetailEnrichment upsertEnrichment(JobEnrichmentKey key) {
        Objects.requireNonNull(key, "enrichment key must not be null");
        if (enrichments == null) {
            enrichments = new LinkedHashSet<>();
        }
        return findEnrichment(key)
                .orElseGet(() -> {
                    JobDetailEnrichment enrichment = new JobDetailEnrichment(this, key);
                    enrichments.add(enrichment);
                    return enrichment;
                });
    }

    public Map<JobEnrichmentKey, JobDetailEnrichment> getEnrichmentsByKey() {
        if (enrichments == null || enrichments.isEmpty()) {
            return Map.of();
        }
        Map<JobEnrichmentKey, JobDetailEnrichment> map = new LinkedHashMap<>();
        for (JobDetailEnrichment enrichment : enrichments) {
            map.put(enrichment.getEnrichmentKey(), enrichment);
        }
        return map;
    }

    public long getContentVersion() {
        return contentVersion;
    }

    public void incrementContentVersion() {
        this.contentVersion = Math.max(0, this.contentVersion) + 1;
    }
}
