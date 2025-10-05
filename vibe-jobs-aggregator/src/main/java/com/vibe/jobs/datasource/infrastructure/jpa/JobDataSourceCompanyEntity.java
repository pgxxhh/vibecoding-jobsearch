package com.vibe.jobs.datasource.infrastructure.jpa;

import com.vibe.jobs.datasource.infrastructure.jpa.converter.JsonStringMapConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "job_data_source_company", indexes = {
        @Index(name = "idx_job_data_source_company_deleted", columnList = "deleted"),
        @Index(name = "idx_job_data_source_company_code_deleted", columnList = "data_source_code, deleted")
})
// 移除 @Where 注解，改用显式的查询过滤
public class JobDataSourceCompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_source_code", nullable = false)
    private String dataSourceCode;

    @Column(nullable = false)
    private String reference;

    @Column(name = "display_name")
    private String displayName;

    @Column
    private String slug;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "placeholder_overrides", columnDefinition = "text")
    @Convert(converter = JsonStringMapConverter.class)
    private Map<String, String> placeholderOverrides = new LinkedHashMap<>();

    @Column(name = "override_options", columnDefinition = "text")
    @Convert(converter = JsonStringMapConverter.class)
    private Map<String, String> overrideOptions = new LinkedHashMap<>();

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDataSourceCode() {
        return dataSourceCode;
    }

    public void setDataSourceCode(String dataSourceCode) {
        this.dataSourceCode = dataSourceCode;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, String> getPlaceholderOverrides() {
        return placeholderOverrides;
    }

    public void setPlaceholderOverrides(Map<String, String> placeholderOverrides) {
        this.placeholderOverrides = placeholderOverrides;
    }

    public Map<String, String> getOverrideOptions() {
        return overrideOptions;
    }

    public void setOverrideOptions(Map<String, String> overrideOptions) {
        this.overrideOptions = overrideOptions;
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
