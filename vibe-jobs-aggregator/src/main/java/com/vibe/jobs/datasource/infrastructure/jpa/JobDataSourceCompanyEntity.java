package com.vibe.jobs.datasource.infrastructure.jpa;

import com.vibe.jobs.datasource.infrastructure.jpa.converter.JsonStringMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "job_data_source_company")
public class JobDataSourceCompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 基于 code 的关联 - 更符合业务语义
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_code", referencedColumnName = "code", nullable = false)
    private JobDataSourceEntity dataSource;

    @Column(name = "data_source_code", nullable = false, insertable = false, updatable = false)
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JobDataSourceEntity getDataSource() {
        return dataSource;
    }

    public void setDataSource(JobDataSourceEntity dataSource) {
        this.dataSource = dataSource;
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
}
