package com.vibe.jobs.jobposting.infrastructure.persistence;

import com.vibe.jobs.jobposting.domain.JobDetail;
import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobDetailRepository extends JpaRepository<JobDetail, Long>, JobDetailRepositoryCustom {

    @EntityGraph(attributePaths = "enrichments")
    @Query("SELECT jd FROM JobDetail jd WHERE jd.job.id = :jobId AND jd.deleted = false")
    Optional<JobDetail> findByJobId(@Param("jobId") Long jobId);

    @Query("SELECT jd.job.id AS jobId, jd.contentText AS contentText FROM JobDetail jd WHERE jd.job.id IN :jobIds AND jd.deleted = false")
    List<ContentTextView> findContentTextByJobIds(@Param("jobIds") Collection<Long> jobIds);

    @Query("SELECT jd.job.id AS jobId, je.enrichmentKey AS enrichmentKey, je.valueJson AS valueJson " +
            "FROM JobDetail jd LEFT JOIN jd.enrichments je " +
            "WHERE jd.job.id IN :jobIds AND jd.deleted = false")
    List<EnrichmentView> findEnrichmentsByJobIds(@Param("jobIds") Collection<Long> jobIds);

    @EntityGraph(attributePaths = "enrichments")
    @Query("SELECT jd FROM JobDetail jd WHERE jd.id = :id AND jd.deleted = false")
    Optional<JobDetail> findByIdWithEnrichments(@Param("id") Long id);

    // 软删除方法 - 避免与JpaRepository的deleteById冲突
    @Modifying
    @Query("UPDATE JobDetail jd SET jd.deleted = true, jd.updatedAt = :deletedAt WHERE jd.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("deletedAt") Instant deletedAt);

    // 根据Job ID软删除
    @Modifying
    @Query("UPDATE JobDetail jd SET jd.deleted = true, jd.updatedAt = :deletedAt WHERE jd.job.id = :jobId")
    void softDeleteByJobId(@Param("jobId") Long jobId, @Param("deletedAt") Instant deletedAt);

    // 查找所有（包括软删除的）
    @Query("SELECT jd FROM JobDetail jd WHERE jd.id = :id")
    Optional<JobDetail> findByIdIncludingDeleted(@Param("id") Long id);

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
