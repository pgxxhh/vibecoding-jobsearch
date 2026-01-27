package com.vibe.jobs.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "company-discovery")
public class CompanyDiscoveryProperties {

    private boolean enabled = false;
    private long fixedDelayMs = 86_400_000L;
    private long initialDelayMs = 10_000L;
    private int pageSize = 50;
    private int maxCandidatesPerRun = 200;
    private boolean dryRun = true;
    private List<String> includeDataSourceTypes = new ArrayList<>();
    private List<String> excludeCompanies = new ArrayList<>();
    private IngestionProperties.LocationFilter locationFilter = new IngestionProperties.LocationFilter();
    private IngestionProperties.RoleFilter roleFilter = new IngestionProperties.RoleFilter();
    private Map<String, ProviderSettings> providers = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = Math.max(fixedDelayMs, 1_000L);
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = Math.max(initialDelayMs, 0L);
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(pageSize, 1);
    }

    public int getMaxCandidatesPerRun() {
        return maxCandidatesPerRun;
    }

    public void setMaxCandidatesPerRun(int maxCandidatesPerRun) {
        this.maxCandidatesPerRun = Math.max(maxCandidatesPerRun, 1);
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public List<String> getIncludeDataSourceTypes() {
        return includeDataSourceTypes;
    }

    public void setIncludeDataSourceTypes(List<String> includeDataSourceTypes) {
        this.includeDataSourceTypes = includeDataSourceTypes == null ? new ArrayList<>() : new ArrayList<>(includeDataSourceTypes);
    }

    public List<String> getExcludeCompanies() {
        return excludeCompanies;
    }

    public void setExcludeCompanies(List<String> excludeCompanies) {
        this.excludeCompanies = excludeCompanies == null ? new ArrayList<>() : new ArrayList<>(excludeCompanies);
    }

    public IngestionProperties.LocationFilter getLocationFilter() {
        return locationFilter;
    }

    public void setLocationFilter(IngestionProperties.LocationFilter locationFilter) {
        this.locationFilter = locationFilter == null ? new IngestionProperties.LocationFilter() : locationFilter;
    }

    public IngestionProperties.RoleFilter getRoleFilter() {
        return roleFilter;
    }

    public void setRoleFilter(IngestionProperties.RoleFilter roleFilter) {
        this.roleFilter = roleFilter == null ? new IngestionProperties.RoleFilter() : roleFilter;
    }

    public Map<String, ProviderSettings> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderSettings> providers) {
        this.providers = providers == null ? new HashMap<>() : new HashMap<>(providers);
    }

    public static class ProviderSettings {
        private boolean enabled = true;
        private String baseUrl;
        private List<String> seedCompanies = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public List<String> getSeedCompanies() {
            return seedCompanies;
        }

        public void setSeedCompanies(List<String> seedCompanies) {
            this.seedCompanies = seedCompanies == null ? new ArrayList<>() : new ArrayList<>(seedCompanies);
        }
    }
}
