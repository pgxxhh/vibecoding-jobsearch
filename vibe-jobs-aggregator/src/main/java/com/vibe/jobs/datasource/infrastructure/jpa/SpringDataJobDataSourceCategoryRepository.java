package com.vibe.jobs.datasource.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SpringDataJobDataSourceCategoryRepository extends JpaRepository<JobDataSourceCategoryEntity, Long> {
    
    @Query("SELECT c FROM JobDataSourceCategoryEntity c WHERE c.dataSourceCode = :dataSourceCode AND c.deleted = false ORDER BY c.name")
    List<JobDataSourceCategoryEntity> findByDataSourceCodeOrderByName(@Param("dataSourceCode") String dataSourceCode);
    
    @Modifying
    @Query("UPDATE JobDataSourceCategoryEntity c SET c.deleted = true, c.updatedTime = :deletedAt WHERE c.dataSourceCode = :dataSourceCode")
    void softDeleteByDataSourceCode(@Param("dataSourceCode") String dataSourceCode, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query("UPDATE JobDataSourceCategoryEntity c SET c.deleted = true, c.updatedTime = :deletedAt WHERE c.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("deletedAt") Instant deletedAt);

    // 保留原有的物理删除方法，供特殊情况使用
    @Modifying
    @Query("DELETE FROM JobDataSourceCategoryEntity c WHERE c.dataSourceCode = :dataSourceCode")
    void hardDeleteByDataSourceCode(@Param("dataSourceCode") String dataSourceCode);
}