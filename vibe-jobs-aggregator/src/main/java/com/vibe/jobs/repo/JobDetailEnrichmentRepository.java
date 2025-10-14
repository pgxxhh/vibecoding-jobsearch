package com.vibe.jobs.repo;

import com.vibe.jobs.domain.JobDetailEnrichment;
import com.vibe.jobs.domain.JobEnrichmentKey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface JobDetailEnrichmentRepository extends JpaRepository<JobDetailEnrichment, Long> {

    @EntityGraph(attributePaths = {"jobDetail", "jobDetail.job"})
    List<JobDetailEnrichment> findByEnrichmentKeyAndStatusStateAndNextRetryAtLessThanEqual(
            JobEnrichmentKey enrichmentKey,
            String statusState,
            Instant nextRetryAt,
            Pageable pageable);

    @Modifying
    @Query("UPDATE JobDetailEnrichment e SET e.statusState = :targetStatus, e.lastAttemptAt = :attemptedAt, " +
            "e.nextRetryAt = NULL WHERE e.id = :id AND e.statusState = :expectedStatus AND e.deleted = false")
    int markRetrying(@Param("id") Long id,
                     @Param("expectedStatus") String expectedStatus,
                     @Param("targetStatus") String targetStatus,
                     @Param("attemptedAt") Instant attemptedAt);
}
