package com.vibe.jobs.jobposting.infrastructure.persistence;

import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;
import com.vibe.jobs.jobposting.infrastructure.persistence.entity.JobDetailJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobDetailJpaRepository extends JpaRepository<JobDetailJpaEntity, Long>, JobDetailJpaRepositoryCustom {

    @EntityGraph(attributePaths = "enrichments")
    @Query("SELECT jd FROM JobDetailJpaEntity jd WHERE jd.job.id = :jobId AND jd.deleted = false")
    Optional<JobDetailJpaEntity> findByJobId(@Param("jobId") Long jobId);

    @Query("SELECT jd.job.id AS jobId, jd.contentText AS contentText FROM JobDetailJpaEntity jd WHERE jd.job.id IN :jobIds AND jd.deleted = false")
    List<ContentTextView> findContentTextByJobIds(@Param("jobIds") Collection<Long> jobIds);

    @Query("SELECT jd.job.id AS jobId, je.enrichmentKey AS enrichmentKey, je.valueJson AS valueJson " +
            "FROM JobDetailJpaEntity jd LEFT JOIN jd.enrichments je " +
            "WHERE jd.job.id IN :jobIds AND jd.deleted = false")
    List<EnrichmentView> findEnrichmentsByJobIds(@Param("jobIds") Collection<Long> jobIds);

    @EntityGraph(attributePaths = "enrichments")
    @Query("SELECT jd FROM JobDetailJpaEntity jd WHERE jd.id = :id AND jd.deleted = false")
    Optional<JobDetailJpaEntity> findByIdWithEnrichments(@Param("id") Long id);

    @Modifying
    @Query("UPDATE JobDetailJpaEntity jd SET jd.deleted = true, jd.updatedAt = :deletedAt WHERE jd.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query("UPDATE JobDetailJpaEntity jd SET jd.deleted = true, jd.updatedAt = :deletedAt WHERE jd.job.id = :jobId")
    void softDeleteByJobId(@Param("jobId") Long jobId, @Param("deletedAt") Instant deletedAt);

    @Query("SELECT jd FROM JobDetailJpaEntity jd WHERE jd.id = :id")
    Optional<JobDetailJpaEntity> findByIdIncludingDeleted(@Param("id") Long id);

    interface ContentTextView {
        Long getJobId();

        String getContentText();
    }

    interface EnrichmentView {
        Long getJobId();

        JobEnrichmentKey getEnrichmentKey();

        String getValueJson();
    }
}
