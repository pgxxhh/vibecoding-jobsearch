package com.vibe.jobs.service.enrichment;

import java.util.List;

public record JobSnapshot(
        Long id,
        String title,
        String company,
        String location,
        String level,
        String url,
        List<String> tags
) {
}
