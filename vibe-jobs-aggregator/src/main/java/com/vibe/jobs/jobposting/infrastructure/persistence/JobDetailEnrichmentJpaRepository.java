package com.vibe.jobs.jobposting.infrastructure.persistence;

import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;
import com.vibe.jobs.jobposting.infrastructure.persistence.entity.JobDetailEnrichmentJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface JobDetailEnrichmentJpaRepository extends JpaRepository<JobDetailEnrichmentJpaEntity, Long> {

    @EntityGraph(attributePaths = {"jobDetail", "jobDetail.job"})
    List<JobDetailEnrichmentJpaEntity> findByEnrichmentKeyAndStatusStateAndNextRetryAtLessThanEqual(
            JobEnrichmentKey enrichmentKey,
            String statusState,
            Instant nextRetryAt,
            Pageable pageable);

    @Modifying
    @Query("UPDATE JobDetailEnrichmentJpaEntity e SET e.statusState = :targetStatus, e.lastAttemptAt = :attemptedAt, " +
            "e.nextRetryAt = NULL WHERE e.id = :id AND e.statusState = :expectedStatus AND e.deleted = false")
    int markRetrying(@Param("id") Long id,
                     @Param("expectedStatus") String expectedStatus,
                     @Param("targetStatus") String targetStatus,
                     @Param("attemptedAt") Instant attemptedAt);
}
