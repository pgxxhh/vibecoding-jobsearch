package com.vibe.jobs.crawler.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CrawlFlow {

    private final List<CrawlStep> steps;

    private CrawlFlow(List<CrawlStep> steps) {
        this.steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public static CrawlFlow of(List<CrawlStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return empty();
        }
        List<CrawlStep> normalized = new ArrayList<>();
        for (CrawlStep step : steps) {
            if (step == null) {
                continue;
            }
            normalized.add(step);
        }
        return new CrawlFlow(Collections.unmodifiableList(normalized));
    }

    public static CrawlFlow empty() {
        return new CrawlFlow(List.of());
    }

    public List<CrawlStep> steps() {
        return steps;
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }
}
