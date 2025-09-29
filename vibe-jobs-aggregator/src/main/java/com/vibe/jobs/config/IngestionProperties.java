package com.vibe.jobs.config;

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
    private Mode mode = Mode.RECENT;
    private List<String> companies = new ArrayList<>();
    private int recentDays = 7;
    private int concurrency = 4;
    private List<Source> sources = new ArrayList<>();

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

}
