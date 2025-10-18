package com.vibe.jobs.jobposting.infrastructure.persistence;

import com.vibe.jobs.jobposting.domain.JobDetail;
import com.vibe.jobs.jobposting.domain.JobDetailEnrichment;
import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;
import com.vibe.jobs.jobposting.domain.spi.JobDetailEnrichmentRepositoryPort;
import com.vibe.jobs.jobposting.infrastructure.persistence.entity.JobDetailEnrichmentJpaEntity;
import com.vibe.jobs.jobposting.infrastructure.persistence.entity.JobDetailJpaEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class JobDetailEnrichmentRepositoryAdapter implements JobDetailEnrichmentRepositoryPort {

    private final JobDetailEnrichmentJpaRepository enrichmentJpaRepository;

    public JobDetailEnrichmentRepositoryAdapter(JobDetailEnrichmentJpaRepository enrichmentJpaRepository) {
        this.enrichmentJpaRepository = enrichmentJpaRepository;
    }

    @Override
    public List<JobDetailEnrichment> findByEnrichmentKeyAndStatusStateAndNextRetryAtLessThanEqual(JobEnrichmentKey enrichmentKey,
                                                                                                  String statusState,
                                                                                                  Instant nextRetryAt,
                                                                                                  int limit) {
        int pageSize = limit > 0 ? limit : Integer.MAX_VALUE;
        List<JobDetailEnrichmentJpaEntity> entities = enrichmentJpaRepository
                .findByEnrichmentKeyAndStatusStateAndNextRetryAtLessThanEqual(
                        enrichmentKey,
                        statusState,
                        nextRetryAt,
                        PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "nextRetryAt")));
        List<JobDetailEnrichment> results = new ArrayList<>(entities.size());
        for (JobDetailEnrichmentJpaEntity entity : entities) {
            JobDetail detail = null;
            JobDetailJpaEntity detailEntity = entity.getJobDetail();
            if (detailEntity != null) {
                detail = detailEntity.toDomain();
            }
            JobDetailEnrichment enrichment = entity.toDomain(detail);
            if (detail != null) {
                detail.setEnrichments(new java.util.LinkedHashSet<>(java.util.List.of(enrichment)));
            }
            results.add(enrichment);
        }
        return results;
    }

    @Override
    @Transactional
    public int markRetrying(Long id, String expectedStatus, String targetStatus, Instant attemptedAt) {
        return enrichmentJpaRepository.markRetrying(id, expectedStatus, targetStatus, attemptedAt);
    }
}
