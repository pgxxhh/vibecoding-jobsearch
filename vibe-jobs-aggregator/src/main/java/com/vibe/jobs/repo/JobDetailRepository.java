package com.vibe.jobs.repo;

import com.vibe.jobs.domain.JobDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface JobDetailRepository extends JpaRepository<JobDetail, Long> {
    
    @Query("SELECT jd FROM JobDetail jd WHERE jd.job.id = :jobId AND jd.deleted = false")
    Optional<JobDetail> findByJobId(@Param("jobId") Long jobId);

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
}
