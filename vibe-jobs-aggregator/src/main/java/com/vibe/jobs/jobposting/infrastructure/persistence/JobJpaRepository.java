
package com.vibe.jobs.jobposting.infrastructure.persistence;

import com.vibe.jobs.jobposting.infrastructure.persistence.entity.JobJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobJpaRepository extends JpaRepository<JobJpaEntity, Long>, JobJpaRepositoryCustom {

    @Query("SELECT j FROM JobJpaEntity j WHERE j.source = :source AND j.externalId = :externalId AND j.deleted = false")
    Optional<JobJpaEntity> findBySourceAndExternalId(@Param("source") String source, @Param("externalId") String externalId);

    @Query("SELECT j FROM JobJpaEntity j WHERE j.deleted = false AND lower(j.company) = lower(:company) AND lower(j.title) = lower(:title) ORDER BY j.createdAt DESC")
    Optional<JobJpaEntity> findTopByCompanyIgnoreCaseAndTitleIgnoreCase(@Param("company") String company, @Param("title") String title);

    @Modifying
    @Query("UPDATE JobJpaEntity j SET j.deleted = true, j.updatedAt = :deletedAt WHERE j.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query("UPDATE JobJpaEntity j SET j.deleted = true, j.updatedAt = :deletedAt WHERE j.id IN :ids")
    void softDeleteByIds(@Param("ids") List<Long> ids, @Param("deletedAt") Instant deletedAt);

    @Query("SELECT j FROM JobJpaEntity j WHERE j.id = :id")
    Optional<JobJpaEntity> findByIdIncludingDeleted(@Param("id") Long id);
}
