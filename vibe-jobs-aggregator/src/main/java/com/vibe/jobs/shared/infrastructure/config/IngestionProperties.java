package com.vibe.jobs.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "ingestion")
public class IngestionProperties {

    private long fixedDelayMs = 3_600_000L;
    private long initialDelayMs = 10_000L;
    private int pageSize = 100;
    private int recentDays = 7;
    private int concurrency = 4;
    private long concurrentSourceTimeoutMs = 300_000L;
    private Map<String, CompanyOverride> companyOverrides = new HashMap<>();
    private LocationFilter locationFilter = new LocationFilter();
    private RoleFilter roleFilter = new RoleFilter();

    // Mode 枚举已弃用，保留仅为兼容性，实际逻辑不再使用
    @Deprecated
    public enum Mode {
        COMPANIES,
        RECENT
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getRecentDays() {
        return recentDays;
    }

    public void setRecentDays(int recentDays) {
        this.recentDays = Math.max(recentDays, 1);
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = Math.max(concurrency, 1);
    }

    public long getConcurrentSourceTimeoutMs() {
        return concurrentSourceTimeoutMs;
    }

    public void setConcurrentSourceTimeoutMs(long concurrentSourceTimeoutMs) {
        this.concurrentSourceTimeoutMs = Math.max(concurrentSourceTimeoutMs, 1_000L);
    }

    public LocationFilter getLocationFilter() {
        return locationFilter;
    }

    public void setLocationFilter(LocationFilter locationFilter) {
        this.locationFilter = locationFilter == null ? new LocationFilter() : locationFilter;
    }

    public RoleFilter getRoleFilter() {
        return roleFilter;
    }

    public void setRoleFilter(RoleFilter roleFilter) {
        this.roleFilter = roleFilter == null ? new RoleFilter() : roleFilter;
    }



    public Map<String, CompanyOverride> getCompanyOverrides() {
        return companyOverrides;
    }

    public void setCompanyOverrides(Map<String, CompanyOverride> companyOverrides) {
        this.companyOverrides = companyOverrides == null ? new HashMap<>() : new HashMap<>(companyOverrides);
    }

    public static class LocationFilter {
        private boolean enabled = false;
        private List<String> includeCountries = new ArrayList<>();
        private List<String> includeRegions = new ArrayList<>();
        private List<String> includeCities = new ArrayList<>();
        private List<String> excludeCountries = new ArrayList<>();
        private List<String> excludeRegions = new ArrayList<>();
        private List<String> excludeCities = new ArrayList<>();
        private List<String> includeKeywords = new ArrayList<>();
        private List<String> excludeKeywords = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getIncludeCountries() {
            return includeCountries;
        }

        public void setIncludeCountries(List<String> includeCountries) {
            this.includeCountries = includeCountries == null ? new ArrayList<>() : new ArrayList<>(includeCountries);
        }

        public List<String> getIncludeRegions() {
            return includeRegions;
        }

        public void setIncludeRegions(List<String> includeRegions) {
            this.includeRegions = includeRegions == null ? new ArrayList<>() : new ArrayList<>(includeRegions);
        }

        public List<String> getIncludeCities() {
            return includeCities;
        }

        public void setIncludeCities(List<String> includeCities) {
            this.includeCities = includeCities == null ? new ArrayList<>() : new ArrayList<>(includeCities);
        }

        public List<String> getExcludeCountries() {
            return excludeCountries;
        }

        public void setExcludeCountries(List<String> excludeCountries) {
            this.excludeCountries = excludeCountries == null ? new ArrayList<>() : new ArrayList<>(excludeCountries);
        }

        public List<String> getExcludeRegions() {
            return excludeRegions;
        }

        public void setExcludeRegions(List<String> excludeRegions) {
            this.excludeRegions = excludeRegions == null ? new ArrayList<>() : new ArrayList<>(excludeRegions);
        }

        public List<String> getExcludeCities() {
            return excludeCities;
        }

        public void setExcludeCities(List<String> excludeCities) {
            this.excludeCities = excludeCities == null ? new ArrayList<>() : new ArrayList<>(excludeCities);
        }

        public List<String> getIncludeKeywords() {
            return includeKeywords;
        }

        public void setIncludeKeywords(List<String> includeKeywords) {
            this.includeKeywords = includeKeywords == null ? new ArrayList<>() : new ArrayList<>(includeKeywords);
        }

        public List<String> getExcludeKeywords() {
            return excludeKeywords;
        }

        public void setExcludeKeywords(List<String> excludeKeywords) {
            this.excludeKeywords = excludeKeywords == null ? new ArrayList<>() : new ArrayList<>(excludeKeywords);
        }

        public boolean matches(String location) {
            if (!enabled || location == null || location.isBlank()) {
                return !enabled;
            }

            String locationLower = location.toLowerCase().trim();

            if (matchesAnyKeyword(locationLower, excludeKeywords) ||
                matchesAnyKeyword(locationLower, excludeCountries) ||
                matchesAnyKeyword(locationLower, excludeRegions) ||
                matchesAnyKeyword(locationLower, excludeCities)) {
                return false;
            }

            if (includeKeywords.isEmpty() && includeCountries.isEmpty() &&
                includeRegions.isEmpty() && includeCities.isEmpty()) {
                return true;
            }

            return matchesAnyKeyword(locationLower, includeKeywords) ||
                   matchesAnyKeyword(locationLower, includeCountries) ||
                   matchesAnyKeyword(locationLower, includeRegions) ||
                   matchesAnyKeyword(locationLower, includeCities);
        }

        private boolean matchesAnyKeyword(String location, List<String> keywords) {
            if (keywords == null || keywords.isEmpty()) {
                return false;
            }
            return keywords.stream()
                    .filter(keyword -> keyword != null && !keyword.isBlank())
                    .anyMatch(keyword -> location.contains(keyword.toLowerCase().trim()));
        }
    }

    public static class RoleFilter {
        private boolean enabled = false;
        private boolean searchDescription = true;
        private List<String> includeKeywords = new ArrayList<>();
        private List<String> excludeKeywords = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isSearchDescription() {
            return searchDescription;
        }

        public void setSearchDescription(boolean searchDescription) {
            this.searchDescription = searchDescription;
        }

        public List<String> getIncludeKeywords() {
            return includeKeywords;
        }

        public void setIncludeKeywords(List<String> includeKeywords) {
            this.includeKeywords = includeKeywords == null ? new ArrayList<>() : new ArrayList<>(includeKeywords);
        }

        public List<String> getExcludeKeywords() {
            return excludeKeywords;
        }

        public void setExcludeKeywords(List<String> excludeKeywords) {
            this.excludeKeywords = excludeKeywords == null ? new ArrayList<>() : new ArrayList<>(excludeKeywords);
        }

        // Helper methods for RoleFilter
        public List<String> getAllowKeywords() { return includeKeywords; }
        public void setAllowKeywords(List<String> allowKeywords) { setIncludeKeywords(allowKeywords); }
        public List<String> getBlockKeywords() { return excludeKeywords; }
        public void setBlockKeywords(List<String> blockKeywords) { setExcludeKeywords(blockKeywords); }
        public List<String> getAllowLevels() { return new ArrayList<>(); }
        public void setAllowLevels(List<String> allowLevels) { /* Not implemented */ }
        public List<String> getBlockLevels() { return new ArrayList<>(); }
        public void setBlockLevels(List<String> blockLevels) { /* Not implemented */ }
        public List<String> getAllowCategories() { return new ArrayList<>(); }
        public void setAllowCategories(List<String> allowCategories) { /* Not implemented */ }
        public List<String> getBlockCategories() { return new ArrayList<>(); }
        public void setBlockCategories(List<String> blockCategories) { /* Not implemented */ }
    }

    public static class CompanyOverride {
        private boolean enabled = true;
        private String displayName;
        private Map<String, Object> sourceSpecificOptions = new HashMap<>();

        public CompanyOverride() {}

        public CompanyOverride(String displayName) {
            this.displayName = displayName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public Map<String, Object> getSourceSpecificOptions() {
            return sourceSpecificOptions;
        }

        public void setSourceSpecificOptions(Map<String, Object> sourceSpecificOptions) {
            this.sourceSpecificOptions = sourceSpecificOptions == null ? new HashMap<>() : new HashMap<>(sourceSpecificOptions);
        }

        public CompanyOverride normalized() {
            CompanyOverride normalized = new CompanyOverride();
            normalized.setEnabled(this.enabled);
            normalized.setDisplayName(this.displayName != null ? this.displayName.trim() : null);
            normalized.setSourceSpecificOptions(new HashMap<>(this.sourceSpecificOptions));
            return normalized;
        }
    }
}
