package com.vibe.jobs.service.enrichment;

import com.vibe.jobs.domain.Job;

import java.util.Optional;

/**
 * Strategy abstraction for job content enrichment backed by different LLM providers.
 */
public interface JobContentEnrichmentProvider {

    /**
     * Unique provider name used for configuration based lookup (case insensitive).
     */
    String name();

    /**
     * Whether the provider is able to serve requests given the current environment/configuration.
     */
    boolean isEnabled();

    /**
     * Enriches the job content returning structured details if available.
     */
    Optional<JobContentEnrichment> enrich(Job job, String rawContent, String contentText);
}
