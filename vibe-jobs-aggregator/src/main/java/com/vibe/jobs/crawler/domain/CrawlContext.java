package com.vibe.jobs.crawler.domain;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CrawlContext {

    private final String dataSourceCode;
    private final String company;
    private final String sourceName;
    private final String entryUrlOverride;
    private final Map<String, String> options;

    public CrawlContext(String dataSourceCode,
                        String company,
                        String sourceName,
                        String entryUrlOverride,
                        Map<String, String> options) {
        this.dataSourceCode = sanitize(dataSourceCode);
        this.company = sanitize(company);
        this.sourceName = sanitize(sourceName);
        this.entryUrlOverride = entryUrlOverride == null ? "" : entryUrlOverride.trim();
        this.options = options == null ? Map.of() : Collections.unmodifiableMap(options);
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    public String dataSourceCode() {
        return dataSourceCode;
    }

    public String company() {
        return company;
    }

    public String sourceName() {
        return sourceName;
    }

    public Optional<String> entryUrlOverride() {
        return entryUrlOverride.isBlank() ? Optional.empty() : Optional.of(entryUrlOverride);
    }

    public Map<String, String> options() {
        return options;
    }

    public String option(String key) {
        Objects.requireNonNull(key, "key");
        return options.getOrDefault(key, "");
    }
}
