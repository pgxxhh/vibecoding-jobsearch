package com.vibe.jobs.jobposting.domain.spi;

import com.vibe.jobs.jobposting.domain.JobDetailEnrichment;
import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;

import java.time.Instant;
import java.util.List;

public interface JobDetailEnrichmentRepositoryPort {

    List<JobDetailEnrichment> findByEnrichmentKeyAndStatusStateAndNextRetryAtLessThanEqual(JobEnrichmentKey enrichmentKey,
                                                                                          String statusState,
                                                                                          Instant nextRetryAt,
                                                                                          int limit);

    int markRetrying(Long id, String expectedStatus, String targetStatus, Instant attemptedAt);
}
