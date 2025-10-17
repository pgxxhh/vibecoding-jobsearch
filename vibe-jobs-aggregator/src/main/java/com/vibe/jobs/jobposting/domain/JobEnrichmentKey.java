package com.vibe.jobs.jobposting.domain;

import java.util.Locale;

public enum JobEnrichmentKey {
    SUMMARY,
    SKILLS,
    HIGHLIGHTS,
    STRUCTURED_DATA,
    STATUS,
    TAGS;

    public String storageKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
