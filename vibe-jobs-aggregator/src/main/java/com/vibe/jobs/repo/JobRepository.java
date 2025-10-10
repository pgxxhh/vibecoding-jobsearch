
package com.vibe.jobs.repo;

import com.vibe.jobs.domain.Job;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JobRepositoryCustom {

    @Query("SELECT j FROM Job j WHERE j.source = :source AND j.externalId = :externalId AND j.deleted = false")
    Job findBySourceAndExternalId(@Param("source") String source, @Param("externalId") String externalId);

    @Query("SELECT j FROM Job j WHERE j.deleted = false AND lower(j.company) = lower(:company) AND lower(j.title) = lower(:title) ORDER BY j.createdAt DESC")
    Optional<Job> findTopByCompanyIgnoreCaseAndTitleIgnoreCase(@Param("company") String company, @Param("title") String title);

    // 软删除方法 - 避免与JpaRepository的deleteById冲突
    @Modifying
    @Query("UPDATE Job j SET j.deleted = true, j.updatedAt = :deletedAt WHERE j.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("deletedAt") Instant deletedAt);

    // 批量软删除
    @Modifying
    @Query("UPDATE Job j SET j.deleted = true, j.updatedAt = :deletedAt WHERE j.id IN :ids")
    void softDeleteByIds(@Param("ids") List<Long> ids, @Param("deletedAt") Instant deletedAt);

    // 查找所有（包括软删除的）
    @Query("SELECT j FROM Job j WHERE j.id = :id")
    Optional<Job> findByIdIncludingDeleted(@Param("id") Long id);
}
