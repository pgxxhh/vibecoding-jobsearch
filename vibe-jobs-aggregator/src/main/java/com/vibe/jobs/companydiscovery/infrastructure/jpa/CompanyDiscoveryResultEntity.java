package com.vibe.jobs.companydiscovery.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "company_discovery_result")
public class CompanyDiscoveryResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "data_source_code", nullable = false)
    private String dataSourceCode;

    @Column(name = "company_reference", nullable = false)
    private String companyReference;

    @Column(name = "display_name")
    private String displayName;

    @Column
    private String provider;

    @Column(nullable = false)
    private String status;

    @Column
    private String reason;

    @Column(name = "create_time", nullable = false)
    private Instant createdTime;

    @Column(name = "update_time", nullable = false)
    private Instant updatedTime;

    @Column(nullable = false)
    private boolean deleted = false;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public String getDataSourceCode() {
        return dataSourceCode;
    }

    public void setDataSourceCode(String dataSourceCode) {
        this.dataSourceCode = dataSourceCode;
    }

    public String getCompanyReference() {
        return companyReference;
    }

    public void setCompanyReference(String companyReference) {
        this.companyReference = companyReference;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
