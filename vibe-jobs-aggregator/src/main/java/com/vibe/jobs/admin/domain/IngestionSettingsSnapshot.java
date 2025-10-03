package com.vibe.jobs.admin.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vibe.jobs.config.IngestionProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class IngestionSettingsSnapshot {

    private final long fixedDelayMs;
    private final long initialDelayMs;
    private final int pageSize;
    private final IngestionProperties.Mode mode;
    private final List<String> companies;
    private final int recentDays;
    private final int concurrency;
    private final Map<String, IngestionProperties.CompanyOverride> companyOverrides;
    private final IngestionProperties.LocationFilter locationFilter;
    private final IngestionProperties.RoleFilter roleFilter;
    private final Instant updatedAt;

    @JsonCreator
    public IngestionSettingsSnapshot(
            @JsonProperty("fixedDelayMs") long fixedDelayMs,
            @JsonProperty("initialDelayMs") long initialDelayMs,
            @JsonProperty("pageSize") int pageSize,
            @JsonProperty("mode") IngestionProperties.Mode mode,
            @JsonProperty("companies") List<String> companies,
            @JsonProperty("recentDays") int recentDays,
            @JsonProperty("concurrency") int concurrency,
            @JsonProperty("companyOverrides") Map<String, IngestionProperties.CompanyOverride> companyOverrides,
            @JsonProperty("locationFilter") IngestionProperties.LocationFilter locationFilter,
            @JsonProperty("roleFilter") IngestionProperties.RoleFilter roleFilter,
            @JsonProperty("updatedAt") Instant updatedAt) {
        this.fixedDelayMs = fixedDelayMs <= 0 ? 3_600_000L : fixedDelayMs;
        this.initialDelayMs = Math.max(0, initialDelayMs);
        this.pageSize = Math.max(1, pageSize);
        this.mode = mode == null ? IngestionProperties.Mode.RECENT : mode;
        this.companies = sanitizeCompanies(companies);
        this.recentDays = Math.max(1, recentDays);
        this.concurrency = Math.max(1, concurrency);
        this.companyOverrides = sanitizeOverrides(companyOverrides);
        this.locationFilter = cloneLocationFilter(locationFilter);
        this.roleFilter = cloneRoleFilter(roleFilter);
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public static IngestionSettingsSnapshot fromProperties(IngestionProperties properties, Instant updatedAt) {
        Objects.requireNonNull(properties, "properties");
        return new IngestionSettingsSnapshot(
                properties.getFixedDelayMs(),
                properties.getInitialDelayMs(),
                properties.getPageSize(),
                properties.getMode(),
                properties.getCompanies(),
                properties.getRecentDays(),
                properties.getConcurrency(),
                properties.getCompanyOverrides(),
                properties.getLocationFilter(),
                properties.getRoleFilter(),
                updatedAt
        );
    }

    public void applyTo(IngestionProperties properties) {
        Objects.requireNonNull(properties, "properties");
        properties.setFixedDelayMs(fixedDelayMs);
        properties.setInitialDelayMs(initialDelayMs);
        properties.setPageSize(pageSize);
        properties.setMode(mode);
        properties.setCompanies(new ArrayList<>(companies));
        properties.setRecentDays(recentDays);
        properties.setConcurrency(concurrency);
        properties.setCompanyOverrides(companyOverrides);
        properties.setLocationFilter(cloneLocationFilter(locationFilter));
        properties.setRoleFilter(cloneRoleFilter(roleFilter));
    }

    public long fixedDelayMs() {
        return fixedDelayMs;
    }

    public long initialDelayMs() {
        return initialDelayMs;
    }

    public int pageSize() {
        return pageSize;
    }

    public IngestionProperties.Mode mode() {
        return mode;
    }

    public List<String> companies() {
        return companies;
    }

    public int recentDays() {
        return recentDays;
    }

    public int concurrency() {
        return concurrency;
    }

    public Map<String, IngestionProperties.CompanyOverride> companyOverrides() {
        return companyOverrides;
    }

    public IngestionProperties.LocationFilter locationFilter() {
        return cloneLocationFilter(locationFilter);
    }

    public IngestionProperties.RoleFilter roleFilter() {
        return cloneRoleFilter(roleFilter);
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static List<String> sanitizeCompanies(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        for (String value : raw) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                sanitized.add(trimmed);
            }
        }
        return List.copyOf(sanitized);
    }

    private static Map<String, IngestionProperties.CompanyOverride> sanitizeOverrides(
            Map<String, IngestionProperties.CompanyOverride> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return Map.of();
        }
        Map<String, IngestionProperties.CompanyOverride> sanitized = new LinkedHashMap<>();
        overrides.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null) {
                return;
            }
            sanitized.put(key.trim().toLowerCase(Locale.ROOT), value.normalized());
        });
        return Map.copyOf(sanitized);
    }

    private static IngestionProperties.LocationFilter cloneLocationFilter(IngestionProperties.LocationFilter source) {
        IngestionProperties.LocationFilter clone = new IngestionProperties.LocationFilter();
        if (source == null) {
            return clone;
        }
        clone.setEnabled(source.isEnabled());
        clone.setIncludeCountries(new ArrayList<>(source.getIncludeCountries()));
        clone.setIncludeRegions(new ArrayList<>(source.getIncludeRegions()));
        clone.setIncludeCities(new ArrayList<>(source.getIncludeCities()));
        clone.setExcludeCountries(new ArrayList<>(source.getExcludeCountries()));
        clone.setExcludeRegions(new ArrayList<>(source.getExcludeRegions()));
        clone.setExcludeCities(new ArrayList<>(source.getExcludeCities()));
        clone.setIncludeKeywords(new ArrayList<>(source.getIncludeKeywords()));
        clone.setExcludeKeywords(new ArrayList<>(source.getExcludeKeywords()));
        return clone;
    }

    private static IngestionProperties.RoleFilter cloneRoleFilter(IngestionProperties.RoleFilter source) {
        IngestionProperties.RoleFilter clone = new IngestionProperties.RoleFilter();
        if (source == null) {
            return clone;
        }
        clone.setEnabled(source.isEnabled());
        clone.setAllowKeywords(new ArrayList<>(source.getAllowKeywords()));
        clone.setBlockKeywords(new ArrayList<>(source.getBlockKeywords()));
        clone.setAllowLevels(new ArrayList<>(source.getAllowLevels()));
        clone.setBlockLevels(new ArrayList<>(source.getBlockLevels()));
        clone.setAllowCategories(new ArrayList<>(source.getAllowCategories()));
        clone.setBlockCategories(new ArrayList<>(source.getBlockCategories()));
        return clone;
    }
}
mkdir -p vibe-jobs-aggregator/src/main/java/com/vibe/jobs/admin/infrastructure/jpa
cat <<'EOF' > vibe-jobs-aggregator/src/main/java/com/vibe/jobs/admin/infrastructure/jpa/IngestionSettingsEntity.java
package com.vibe.jobs.admin.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ingestion_settings")
public class IngestionSettingsEntity {

    @Id
    private Long id;

    @Column(name = "settings_json", nullable = false, columnDefinition = "TEXT")
    private String settingsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public IngestionSettingsEntity() {
    }

    public IngestionSettingsEntity(Long id, String settingsJson) {
        this.id = id;
        this.settingsJson = settingsJson;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
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
}
