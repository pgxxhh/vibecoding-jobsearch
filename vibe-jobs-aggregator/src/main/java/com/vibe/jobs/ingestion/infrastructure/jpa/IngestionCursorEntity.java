package com.vibe.jobs.ingestion.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "ingestion_cursors", uniqueConstraints = {
        @UniqueConstraint(name = "uq_ingestion_cursor", columnNames = {"source_name", "company", "category"})
})
public class IngestionCursorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_code", nullable = false, length = 128)
    private String sourceCode;

    @Column(name = "source_name", nullable = false, length = 128)
    private String sourceName;

    @Column(name = "company", nullable = false, length = 128)
    private String company;

    @Column(name = "category", nullable = false, length = 128)
    private String category;

    @Column(name = "last_posted_at")
    private Instant lastPostedAt;

    @Column(name = "last_external_id", length = 256)
    private String lastExternalId;

    @Column(name = "next_page_token", length = 512)
    private String nextPageToken;

    @Column(name = "last_ingested_at", nullable = false)
    private Instant lastIngestedAt;

    @Column(name = "create_time", nullable = false, updatable = false)
    private Instant createTime;

    @Column(name = "update_time", nullable = false)
    private Instant updateTime;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public IngestionCursorEntity() {
    }

    @PrePersist
    public void onInsert() {
        if (lastIngestedAt == null) {
            lastIngestedAt = Instant.now();
        }
        if (createTime == null) {
            createTime = Instant.now();
        }
        if (updateTime == null) {
            updateTime = Instant.now();
        }
        if (company == null) {
            company = "";
        }
        if (category == null) {
            category = "";
        }
    }

    @PreUpdate
    public void onUpdate() {
        lastIngestedAt = Instant.now();
        updateTime = Instant.now();
        if (createTime == null) {
            createTime = updateTime;
        }
        if (company == null) {
            company = "";
        }
        if (category == null) {
            category = "";
        }
    }

    public Long getId() {
        return id;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company == null ? "" : company;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null ? "" : category;
    }

    public Instant getLastPostedAt() {
        return lastPostedAt;
    }

    public void setLastPostedAt(Instant lastPostedAt) {
        this.lastPostedAt = lastPostedAt;
    }

    public String getLastExternalId() {
        return lastExternalId;
    }

    public void setLastExternalId(String lastExternalId) {
        this.lastExternalId = lastExternalId;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }

    public Instant getLastIngestedAt() {
        return lastIngestedAt;
    }

    public void setLastIngestedAt(Instant lastIngestedAt) {
        this.lastIngestedAt = lastIngestedAt;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
