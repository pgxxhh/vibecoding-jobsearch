package com.vibe.jobs.crawler.domain;

import java.util.Collections;
import java.util.Map;

public class CrawlStep {
    private final CrawlStepType type;
    private final Map<String, Object> options;

    public CrawlStep(CrawlStepType type, Map<String, Object> options) {
        this.type = type == null ? CrawlStepType.REQUEST : type;
        this.options = options == null ? Map.of() : Collections.unmodifiableMap(options);
    }

    public static CrawlStep of(CrawlStepType type, Map<String, Object> options) {
        return new CrawlStep(type, options);
    }

    public CrawlStepType type() {
        return type;
    }

    public Map<String, Object> options() {
        return options;
    }
}
