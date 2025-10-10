package com.vibe.jobs.datasource.infrastructure.jpa;

import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.infrastructure.jpa.converter.JsonMixedTypeMapConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "job_data_source", indexes = {
        @Index(name = "idx_job_data_source_deleted", columnList = "deleted")
})
@Where(clause = "deleted = false")
public class JobDataSourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "run_on_startup", nullable = false)
    private boolean runOnStartup;

    @Column(name = "require_override", nullable = false)
    private boolean requireOverride;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobDataSource.Flow flow = JobDataSource.Flow.UNLIMITED;

    @Column(name = "base_options", columnDefinition = "text")
    @jakarta.persistence.Convert(converter = JsonMixedTypeMapConverter.class)
    private Map<String, String> baseOptions = new LinkedHashMap<>();

    // 软删除字段
    @Column(nullable = false)
    private boolean deleted = false;

    // 时间字段
    @Column(name = "created_time", nullable = false, updatable = false)
    private Instant createdTime;

    @Column(name = "updated_time", nullable = false)
    private Instant updatedTime;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdTime = now;
        updatedTime = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedTime = Instant.now();
    }

    public void delete() {
        this.deleted = true;
        this.updatedTime = Instant.now();
    }

    public boolean isNotDeleted() {
        return !deleted;
    }

    // Note: Companies and categories are managed manually in the repository
    // to avoid JPA association complexity with code-based foreign keys

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRunOnStartup() {
        return runOnStartup;
    }

    public void setRunOnStartup(boolean runOnStartup) {
        this.runOnStartup = runOnStartup;
    }

    public boolean isRequireOverride() {
        return requireOverride;
    }

    public void setRequireOverride(boolean requireOverride) {
        this.requireOverride = requireOverride;
    }

    public JobDataSource.Flow getFlow() {
        return flow;
    }

    public void setFlow(JobDataSource.Flow flow) {
        this.flow = flow;
    }

    public Map<String, String> getBaseOptions() {
        return baseOptions;
    }

    public void setBaseOptions(Map<String, String> baseOptions) {
        this.baseOptions = baseOptions;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Instant createdTime) {
        this.createdTime = createdTime;
    }

    public Instant getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Instant updatedTime) {
        this.updatedTime = updatedTime;
    }
}
