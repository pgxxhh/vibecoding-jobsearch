package com.vibe.jobs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ConfigurationProperties(prefix = "ingestion")
public class IngestionProperties {

    private long fixedDelayMs = 3_600_000L;
    private long initialDelayMs = 10_000L;
    private int pageSize = 100;
    private Mode mode = Mode.RECENT;
    private List<String> companies = new ArrayList<>();
    private int recentDays = 7;
    private int concurrency = 4;
    private List<Source> sources = new ArrayList<>();
    private Map<String, CompanyOverride> companyOverrides = new HashMap<>();
    private LocationFilter locationFilter = new LocationFilter();
    private RoleFilter roleFilter = new RoleFilter();

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

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        if (mode != null) {
            this.mode = mode;
        }
    }

    public List<String> getCompanies() {
        return companies;
    }

    public void setCompanies(List<String> companies) {
        this.companies = companies == null ? new ArrayList<>() : companies;
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

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources == null ? new ArrayList<>() : sources;
    }

    public Map<String, CompanyOverride> getCompanyOverrides() {
        return companyOverrides;
    }

    public void setCompanyOverrides(Map<String, CompanyOverride> companyOverrides) {
        this.companyOverrides = new HashMap<>();
        if (companyOverrides == null) {
            return;
        }
        companyOverrides.forEach((key, value) -> {
            String normalized = normalizeKey(key);
            if (!normalized.isEmpty() && value != null) {
                this.companyOverrides.put(normalized, value.normalized());
            }
        });
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

    public Map<String, String> getPlaceholderOverrides(String companyName) {
        CompanyOverride override = findCompanyOverride(companyName);
        if (override == null) {
            return Map.of();
        }
        return override.placeholderCopy();
    }

    public SourceOverride getSourceOverride(String companyName, String sourceType) {
        CompanyOverride override = findCompanyOverride(companyName);
        if (override == null) {
            return null;
        }
        return override.sourceOverride(sourceType);
    }

    private CompanyOverride findCompanyOverride(String companyName) {
        if (companyOverrides.isEmpty()) {
            return null;
        }
        String normalized = normalizeKey(companyName);
        if (normalized.isEmpty()) {
            return null;
        }
        return companyOverrides.get(normalized);
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public List<String> normalizedCompanies() {
        if (companies == null || companies.isEmpty()) {
            return List.of();
        }
        return companies.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toLowerCase())
                .toList();
    }

    public static class Source {
        private String id;
        private String type;
        private boolean enabled = true;
        private boolean runOnStartup = true;
        private boolean requireOverride = false;
        private Map<String, String> options = new HashMap<>();
        private List<CategoryQuota> categories = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isRunOnStartup() {
            return runOnStartup;
        }

        public void setRunOnStartup(boolean runOnStartup) {
            this.runOnStartup = runOnStartup;
        }

        public boolean isRequireOverride() {
            return requireOverride;
        }

        public void setRequireOverride(boolean requireOverride) {
            this.requireOverride = requireOverride;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        public void setOptions(Map<String, String> options) {
            this.options = options == null ? new HashMap<>() : options;
        }

        public List<CategoryQuota> getCategories() {
            return categories;
        }

        public void setCategories(List<CategoryQuota> categories) {
            this.categories = categories == null ? new ArrayList<>() : new ArrayList<>(categories);
        }

        public String key() {
            if (id != null && !id.isBlank()) {
                return id;
            }
            String typePart = type == null ? "unknown" : type.toLowerCase();
            return typePart + options.hashCode();
        }

        public String displayName() {
            if (id != null && !id.isBlank()) {
                return id;
            }
            if (type == null || type.isBlank()) {
                return "unknown";
            }
            return type.toLowerCase();
        }

        public static class CategoryQuota {
            private String name;
            private int limit;
            private List<String> tags = new ArrayList<>();
            private Map<String, List<String>> facets = new HashMap<>();

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public int getLimit() {
                return Math.max(limit, 0);
            }

            public void setLimit(int limit) {
                this.limit = Math.max(limit, 0);
            }

            public List<String> getTags() {
                return tags;
            }

            public void setTags(List<String> tags) {
                this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
            }

            public Map<String, List<String>> getFacets() {
                return facets;
            }

            public void setFacets(Map<String, List<String>> facets) {
                this.facets = new HashMap<>();
                if (facets == null) {
                    return;
                }
                facets.forEach((key, values) -> {
                    List<String> normalized = values == null ? new ArrayList<>() : new ArrayList<>(values);
                    this.facets.put(key, normalized);
                });
            }
        }
    }

    public static class CompanyOverride {
        private Map<String, String> placeholders = new LinkedHashMap<>();
        private Map<String, SourceOverride> sources = new HashMap<>();

        public Map<String, String> getPlaceholders() {
            return placeholders;
        }

        public void setPlaceholders(Map<String, String> placeholders) {
            this.placeholders = sanitize(placeholders);
        }

        public Map<String, SourceOverride> getSources() {
            return sources;
        }

        public void setSources(Map<String, SourceOverride> sources) {
            this.sources = new HashMap<>();
            if (sources == null) {
                return;
            }
            sources.forEach((key, value) -> {
                String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
                if (!normalized.isEmpty() && value != null) {
                    this.sources.put(normalized, value.normalized());
                }
            });
        }

        private Map<String, String> sanitize(Map<String, String> values) {
            Map<String, String> result = new LinkedHashMap<>();
            if (values == null) {
                return result;
            }
            values.forEach((key, value) -> {
                if (key == null || key.isBlank() || value == null) {
                    return;
                }
                result.put(key.trim(), value.trim());
            });
            return result;
        }

        public Map<String, String> placeholderCopy() {
            return new LinkedHashMap<>(placeholders);
        }

        public SourceOverride sourceOverride(String type) {
            if (type == null) {
                return null;
            }
            return sources.get(type.trim().toLowerCase(Locale.ROOT));
        }

        public CompanyOverride normalized() {
            CompanyOverride copy = new CompanyOverride();
            copy.placeholders = placeholderCopy();
            copy.sources = new HashMap<>();
            sources.forEach((key, value) -> copy.sources.put(key, value.normalized()));
            return copy;
        }
    }

    public static class SourceOverride {
        private boolean enabled = true;
        private Map<String, String> options = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        public void setOptions(Map<String, String> options) {
            this.options = new LinkedHashMap<>();
            if (options == null) {
                return;
            }
            options.forEach((key, value) -> {
                if (key == null || key.isBlank() || value == null) {
                    return;
                }
                this.options.put(key.trim(), value.trim());
            });
        }

        public Map<String, String> optionsCopy() {
            return new LinkedHashMap<>(options);
        }

        private SourceOverride normalized() {
            SourceOverride copy = new SourceOverride();
            copy.enabled = enabled;
            copy.options = optionsCopy();
            return copy;
        }
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

        /**
         * 检查location是否符合过滤条件
         */
        public boolean matches(String location) {
            if (!enabled || location == null || location.isBlank()) {
                return !enabled; // 如果没有启用过滤，则接受所有；如果启用了但location为空，则拒绝
            }

            String locationLower = location.toLowerCase().trim();

            // 检查排除条件 - 如果匹配任何排除条件，直接拒绝
            if (matchesAnyKeyword(locationLower, excludeKeywords) ||
                matchesAnyKeyword(locationLower, excludeCountries) ||
                matchesAnyKeyword(locationLower, excludeRegions) ||
                matchesAnyKeyword(locationLower, excludeCities)) {
                return false;
            }

            // 如果没有任何包含条件，接受（只要不在排除列表中）
            if (includeKeywords.isEmpty() && includeCountries.isEmpty() && 
                includeRegions.isEmpty() && includeCities.isEmpty()) {
                return true;
            }

            // 检查包含条件 - 需要匹配至少一个包含条件
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
    }

}
