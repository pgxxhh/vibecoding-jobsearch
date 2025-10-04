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

    @JsonProperty("fixedDelayMs")
    public long fixedDelayMs() {
        return fixedDelayMs;
    }

    @JsonProperty("initialDelayMs")
    public long initialDelayMs() {
        return initialDelayMs;
    }

    @JsonProperty("pageSize")
    public int pageSize() {
        return pageSize;
    }

    @JsonProperty("mode")
    public IngestionProperties.Mode mode() {
        return mode;
    }

    @JsonProperty("companies")
    public List<String> companies() {
        return companies;
    }

    @JsonProperty("recentDays")
    public int recentDays() {
        return recentDays;
    }

    @JsonProperty("concurrency")
    public int concurrency() {
        return concurrency;
    }

    @JsonProperty("companyOverrides")
    public Map<String, IngestionProperties.CompanyOverride> companyOverrides() {
        return companyOverrides;
    }

    @JsonProperty("locationFilter")
    public IngestionProperties.LocationFilter locationFilter() {
        return cloneLocationFilter(locationFilter);
    }

    @JsonProperty("roleFilter")
    public IngestionProperties.RoleFilter roleFilter() {
        return cloneRoleFilter(roleFilter);
    }

    @JsonProperty("updatedAt")
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
