package com.vibe.jobs.datasource.infrastructure.jpa;

import com.vibe.jobs.datasource.infrastructure.jpa.converter.JsonStringListConverter;
import com.vibe.jobs.datasource.infrastructure.jpa.converter.JsonStringListMapConverter;
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
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "job_data_source_category")
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
}
