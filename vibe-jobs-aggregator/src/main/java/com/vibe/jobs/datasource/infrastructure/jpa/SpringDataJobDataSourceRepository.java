package com.vibe.jobs.datasource.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataJobDataSourceRepository extends JpaRepository<JobDataSourceEntity, Long> {

    @Query("SELECT ds FROM JobDataSourceEntity ds WHERE ds.enabled = true AND ds.deleted = false")
    List<JobDataSourceEntity> findAllEnabled();

    @Query("SELECT ds FROM JobDataSourceEntity ds WHERE ds.deleted = false")
    List<JobDataSourceEntity> findAllNotDeleted();

    @Override
    @Query("SELECT ds FROM JobDataSourceEntity ds")
    List<JobDataSourceEntity> findAll();

    @Query("SELECT CASE WHEN COUNT(ds) > 0 THEN true ELSE false END FROM JobDataSourceEntity ds WHERE ds.code = :code AND ds.deleted = false")
    boolean existsByCode(@Param("code") String code);

    @Query("SELECT ds FROM JobDataSourceEntity ds WHERE ds.code = :code AND ds.deleted = false")
    Optional<JobDataSourceEntity> findByCode(@Param("code") String code);

    // 软删除方法 - 避免与JpaRepository的deleteById冲突
    @Modifying
    @Query("UPDATE JobDataSourceEntity ds SET ds.deleted = true, ds.updatedTime = :deletedAt WHERE ds.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("deletedAt") Instant deletedAt);

    // 查找所有（包括软删除的）
    @Query("SELECT ds FROM JobDataSourceEntity ds WHERE ds.id = :id")
    Optional<JobDataSourceEntity> findByIdIncludingDeleted(@Param("id") Long id);

    @Query("SELECT ds FROM JobDataSourceEntity ds WHERE ds.code = :code")
    Optional<JobDataSourceEntity> findByCodeIncludingDeleted(@Param("code") String code);
}
