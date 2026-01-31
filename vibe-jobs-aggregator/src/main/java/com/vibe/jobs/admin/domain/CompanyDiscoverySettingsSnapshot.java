package com.vibe.jobs.admin.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vibe.jobs.shared.infrastructure.config.CompanyDiscoveryProperties;
import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class CompanyDiscoverySettingsSnapshot {

    private final boolean enabled;
    private final long fixedDelayMs;
    private final long initialDelayMs;
    private final int pageSize;
    private final int maxCandidatesPerRun;
    private final boolean dryRun;
    private final List<String> includeDataSourceTypes;
    private final List<String> excludeCompanies;
    private final Map<String, CompanyDiscoveryProperties.ProviderSettings> providers;
    private final IngestionProperties.LocationFilter locationFilter;
    private final IngestionProperties.RoleFilter roleFilter;
    private final Instant updatedAt;

    @JsonCreator
    public CompanyDiscoverySettingsSnapshot(
            @JsonProperty("enabled") boolean enabled,
            @JsonProperty("fixedDelayMs") long fixedDelayMs,
            @JsonProperty("initialDelayMs") long initialDelayMs,
            @JsonProperty("pageSize") int pageSize,
            @JsonProperty("maxCandidatesPerRun") int maxCandidatesPerRun,
            @JsonProperty("dryRun") boolean dryRun,
            @JsonProperty("includeDataSourceTypes") List<String> includeDataSourceTypes,
            @JsonProperty("excludeCompanies") List<String> excludeCompanies,
            @JsonProperty("providers") Map<String, CompanyDiscoveryProperties.ProviderSettings> providers,
            @JsonProperty("locationFilter") IngestionProperties.LocationFilter locationFilter,
            @JsonProperty("roleFilter") IngestionProperties.RoleFilter roleFilter,
            @JsonProperty("updatedAt") Instant updatedAt) {
        this.enabled = enabled;
        this.fixedDelayMs = Math.max(fixedDelayMs, 1_000L);
        this.initialDelayMs = Math.max(initialDelayMs, 0L);
        this.pageSize = Math.max(pageSize, 1);
        this.maxCandidatesPerRun = Math.max(maxCandidatesPerRun, 1);
        this.dryRun = dryRun;
        this.includeDataSourceTypes = sanitizeList(includeDataSourceTypes);
        this.excludeCompanies = sanitizeList(excludeCompanies);
        this.providers = sanitizeProviders(providers);
        this.locationFilter = cloneLocationFilter(locationFilter);
        this.roleFilter = cloneRoleFilter(roleFilter);
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public static CompanyDiscoverySettingsSnapshot fromProperties(CompanyDiscoveryProperties properties, Instant updatedAt) {
        Objects.requireNonNull(properties, "properties");
        return new CompanyDiscoverySettingsSnapshot(
                properties.isEnabled(),
                properties.getFixedDelayMs(),
                properties.getInitialDelayMs(),
                properties.getPageSize(),
                properties.getMaxCandidatesPerRun(),
                properties.isDryRun(),
                properties.getIncludeDataSourceTypes(),
                properties.getExcludeCompanies(),
                properties.getProviders(),
                properties.getLocationFilter(),
                properties.getRoleFilter(),
                updatedAt
        );
    }

    public void applyTo(CompanyDiscoveryProperties properties) {
        Objects.requireNonNull(properties, "properties");
        properties.setEnabled(enabled);
        properties.setFixedDelayMs(fixedDelayMs);
        properties.setInitialDelayMs(initialDelayMs);
        properties.setPageSize(pageSize);
        properties.setMaxCandidatesPerRun(maxCandidatesPerRun);
        properties.setDryRun(dryRun);
        properties.setIncludeDataSourceTypes(includeDataSourceTypes);
        properties.setExcludeCompanies(excludeCompanies);
        properties.setProviders(new LinkedHashMap<>(providers));
        properties.setLocationFilter(cloneLocationFilter(locationFilter));
        properties.setRoleFilter(cloneRoleFilter(roleFilter));
    }

    @JsonProperty("enabled")
    public boolean enabled() {
        return enabled;
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

    @JsonProperty("maxCandidatesPerRun")
    public int maxCandidatesPerRun() {
        return maxCandidatesPerRun;
    }

    @JsonProperty("dryRun")
    public boolean dryRun() {
        return dryRun;
    }

    @JsonProperty("includeDataSourceTypes")
    public List<String> includeDataSourceTypes() {
        return List.copyOf(includeDataSourceTypes);
    }

    @JsonProperty("excludeCompanies")
    public List<String> excludeCompanies() {
        return List.copyOf(excludeCompanies);
    }

    @JsonProperty("providers")
    public Map<String, CompanyDiscoveryProperties.ProviderSettings> providers() {
        return Map.copyOf(providers);
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

    private static List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static Map<String, CompanyDiscoveryProperties.ProviderSettings> sanitizeProviders(
            Map<String, CompanyDiscoveryProperties.ProviderSettings> providers) {
        if (providers == null || providers.isEmpty()) {
            return Map.of();
        }
        Map<String, CompanyDiscoveryProperties.ProviderSettings> sanitized = new LinkedHashMap<>();
        providers.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null) {
                return;
            }
            sanitized.put(key.trim().toLowerCase(Locale.ROOT), value);
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
        clone.setSearchDescription(source.isSearchDescription());
        clone.setIncludeKeywords(new ArrayList<>(source.getIncludeKeywords()));
        clone.setExcludeKeywords(new ArrayList<>(source.getExcludeKeywords()));
        return clone;
    }
}
