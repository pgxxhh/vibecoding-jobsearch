package com.vibe.jobs.jobposting.infrastructure.persistence.entity;

import com.vibe.jobs.jobposting.domain.Job;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_source_extid", columnList = "source, externalId, deleted"),
        @Index(name = "idx_jobs_title", columnList = "title"),
        @Index(name = "idx_jobs_company", columnList = "company"),
        @Index(name = "idx_jobs_location", columnList = "location"),
        @Index(name = "idx_jobs_deleted", columnList = "deleted")
})
@Where(clause = "deleted = false")
public class JobJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    private String location;
    private String level;

    @Column(columnDefinition = "timestamp")
    private Instant postedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_tags", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @Column(length = 1024)
    private String url;

    @Column(nullable = false, updatable = false, columnDefinition = "timestamp")
    private Instant createdAt;

    @Column(nullable = false, columnDefinition = "timestamp")
    private Instant updatedAt;

    @Column(length = 64)
    private String checksum;

    @Column(nullable = false)
    private boolean deleted = false;

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

    public Job toDomain() {
        Job job = Job.builder()
                .id(id)
                .source(source)
                .externalId(externalId)
                .title(title)
                .company(company)
                .location(location)
                .level(level)
                .postedAt(postedAt)
                .tags(tags != null ? new HashSet<>(tags) : new HashSet<>())
                .url(url)
                .checksum(checksum)
                .deleted(deleted)
                .build();
        job.setCreatedAt(createdAt);
        job.setUpdatedAt(updatedAt);
        return job;
    }

    public static JobJpaEntity fromDomain(Job job) {
        JobJpaEntity entity = new JobJpaEntity();
        entity.updateFromDomain(job);
        return entity;
    }

    public void updateFromDomain(Job job) {
        this.id = job.getId();
        this.source = job.getSource();
        this.externalId = job.getExternalId();
        this.title = job.getTitle();
        this.company = job.getCompany();
        this.location = job.getLocation();
        this.level = job.getLevel();
        this.postedAt = job.getPostedAt();
        this.tags = job.getTags() != null ? new HashSet<>(job.getTags()) : new HashSet<>();
        this.url = job.getUrl();
        this.createdAt = job.getCreatedAt();
        this.updatedAt = job.getUpdatedAt();
        this.checksum = job.getChecksum();
        this.deleted = job.isDeleted();
    }

    public void syncToDomain(Job job) {
        job.setId(id);
        job.setSource(source);
        job.setExternalId(externalId);
        job.setTitle(title);
        job.setCompany(company);
        job.setLocation(location);
        job.setLevel(level);
        job.setPostedAt(postedAt);
        job.setTags(tags != null ? new HashSet<>(tags) : new HashSet<>());
        job.setUrl(url);
        job.setCreatedAt(createdAt);
        job.setUpdatedAt(updatedAt);
        job.setChecksum(checksum);
        job.setDeleted(deleted);
    }

    public Long getId() {
        return id;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markDeleted(Instant deletedAt) {
        this.deleted = true;
        this.updatedAt = Objects.requireNonNullElseGet(deletedAt, Instant::now);
    }
}
