package com.vibe.jobs.jobposting.infrastructure.persistence.entity;

import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.jobposting.domain.JobDetail;
import com.vibe.jobs.jobposting.domain.JobDetailEnrichment;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "job_details", indexes = {
        @Index(name = "idx_job_details_job_id", columnList = "job_id", unique = true),
        @Index(name = "idx_job_details_deleted", columnList = "deleted")
})
@Where(clause = "deleted = false")
public class JobDetailJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private JobJpaEntity job;

    @Column(columnDefinition = "longtext")
    private String content;

    @Column(name = "content_text", columnDefinition = "longtext")
    private String contentText;

    @OneToMany(mappedBy = "jobDetail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Where(clause = "deleted = 0")
    private Set<JobDetailEnrichmentJpaEntity> enrichments = new LinkedHashSet<>();

    @Column(nullable = false, columnDefinition = "timestamp")
    private Instant createdAt;

    @Column(nullable = false, columnDefinition = "timestamp")
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "content_version", nullable = false)
    private long contentVersion = 0L;

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

    public JobDetail toDomain() {
        JobDetail detail = new JobDetail();
        detail.setId(id);
        detail.setJob(job != null ? job.toDomain() : null);
        detail.setContent(content);
        detail.setContentText(contentText);
        detail.setCreatedAt(createdAt);
        detail.setUpdatedAt(updatedAt);
        detail.setDeleted(deleted);
        detail.setContentVersion(contentVersion);
        if (enrichments != null && !enrichments.isEmpty()) {
            Set<JobDetailEnrichment> mapped = enrichments.stream()
                    .map(entity -> entity.toDomain(detail))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            detail.setEnrichments(mapped);
        }
        return detail;
    }

    public static JobDetailJpaEntity fromDomain(JobDetail detail, JobJpaEntity jobEntity) {
        JobDetailJpaEntity entity = new JobDetailJpaEntity();
        entity.updateFromDomain(detail, jobEntity);
        return entity;
    }

    public void updateFromDomain(JobDetail detail, JobJpaEntity jobEntity) {
        this.id = detail.getId();
        this.job = jobEntity;
        this.content = detail.getContent();
        this.contentText = detail.getContentText();
        this.createdAt = detail.getCreatedAt();
        this.updatedAt = detail.getUpdatedAt();
        this.deleted = detail.isDeleted();
        this.contentVersion = detail.getContentVersion();
        syncEnrichments(detail.getEnrichments());
    }

    private void syncEnrichments(Set<JobDetailEnrichment> domainEnrichments) {
        if (this.enrichments == null) {
            this.enrichments = new LinkedHashSet<>();
        }
        Set<JobDetailEnrichmentJpaEntity> updated = new LinkedHashSet<>();
        if (domainEnrichments != null) {
            for (JobDetailEnrichment enrichment : domainEnrichments) {
                JobDetailEnrichmentJpaEntity entity = findExistingEnrichment(enrichment);
                if (entity == null) {
                    entity = JobDetailEnrichmentJpaEntity.fromDomain(enrichment);
                } else {
                    entity.updateFromDomain(enrichment);
                }
                entity.setJobDetail(this);
                updated.add(entity);
            }
        }
        this.enrichments.clear();
        this.enrichments.addAll(updated);
    }

    private JobDetailEnrichmentJpaEntity findExistingEnrichment(JobDetailEnrichment enrichment) {
        if (enrichment.getId() != null) {
            for (JobDetailEnrichmentJpaEntity entity : enrichments) {
                if (Objects.equals(entity.getId(), enrichment.getId())) {
                    return entity;
                }
            }
        }
        if (enrichment.getEnrichmentKey() != null) {
            for (JobDetailEnrichmentJpaEntity entity : enrichments) {
                if (enrichment.getEnrichmentKey() == entity.getEnrichmentKey()) {
                    return entity;
                }
            }
        }
        return null;
    }

    public JobJpaEntity getJob() {
        return job;
    }

    public Long getId() {
        return id;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void markDeleted(Instant deletedAt) {
        this.deleted = true;
        this.updatedAt = deletedAt != null ? deletedAt : Instant.now();
    }
}
