package com.vibe.jobs.service.enrichment;

import java.util.List;

public record JobContentEnrichment(
        String summary,
        List<String> skills,
        List<String> highlights,
        String structuredData
) {
    public JobContentEnrichment {
        skills = skills == null ? List.of() : List.copyOf(skills);
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
    }
}
