package com.vibe.jobs.jobposting.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class JobDetail {

    private Long id;
    private Job job;
    private String content;
    private String contentText;
    private Set<JobDetailEnrichment> enrichments = new LinkedHashSet<>();
    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted = false;
    private long contentVersion = 0L;

    public JobDetail() {
    }

    public JobDetail(Job job, String content, String contentText) {
        this.job = job;
        this.content = content;
        this.contentText = contentText;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Set<JobDetailEnrichment> getEnrichments() {
        return enrichments;
    }

    public void setEnrichments(Set<JobDetailEnrichment> enrichments) {
        this.enrichments = enrichments != null ? enrichments : new LinkedHashSet<>();
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

    public boolean isNotDeleted() {
        return !deleted;
    }

    public long getContentVersion() {
        return contentVersion;
    }

    public void setContentVersion(long contentVersion) {
        this.contentVersion = contentVersion;
    }

    public void incrementContentVersion() {
        this.contentVersion = Math.max(0, this.contentVersion) + 1;
    }

    public void markCreated(Instant timestamp) {
        Instant safeTimestamp = timestamp != null ? timestamp : Instant.now();
        this.createdAt = safeTimestamp;
        this.updatedAt = safeTimestamp;
    }

    public void markUpdated(Instant timestamp) {
        this.updatedAt = timestamp != null ? timestamp : Instant.now();
    }

    public void delete() {
        delete(null);
    }

    public void delete(Instant deletedAt) {
        this.deleted = true;
        this.updatedAt = deletedAt != null ? deletedAt : Instant.now();
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
}
