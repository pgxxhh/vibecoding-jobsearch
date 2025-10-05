package com.vibe.jobs.datasource.infrastructure.jpa;

import com.vibe.jobs.datasource.infrastructure.jpa.converter.JsonStringListConverter;
import com.vibe.jobs.datasource.infrastructure.jpa.converter.JsonStringListMapConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "job_data_source_category", indexes = {
        @Index(name = "idx_job_data_source_category_deleted", columnList = "deleted")
})
@Where(clause = "deleted = false")
public class JobDataSourceCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_source_code", nullable = false)
    private String dataSourceCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "quota_limit", nullable = false)
    private int limit;

    @Column(columnDefinition = "text")
    @Convert(converter = JsonStringListConverter.class)
    private List<String> tags = List.of();

    @Column(columnDefinition = "text")
    @Convert(converter = JsonStringListMapConverter.class)
    private Map<String, List<String>> facets = new LinkedHashMap<>();

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, List<String>> getFacets() {
        return facets;
    }

    public void setFacets(Map<String, List<String>> facets) {
        this.facets = facets;
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
