package com.vibe.jobs.datasource.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SpringDataJobDataSourceCompanyRepository extends JpaRepository<JobDataSourceCompanyEntity, Long> {
    
    @Query("SELECT c FROM JobDataSourceCompanyEntity c WHERE c.dataSourceCode = :dataSourceCode AND c.deleted = false ORDER BY c.reference")
    List<JobDataSourceCompanyEntity> findByDataSourceCodeOrderByReference(@Param("dataSourceCode") String dataSourceCode);
    
    @Modifying
    @Query("UPDATE JobDataSourceCompanyEntity c SET c.deleted = true, c.updatedTime = :deletedAt WHERE c.dataSourceCode = :dataSourceCode")
    void softDeleteByDataSourceCode(@Param("dataSourceCode") String dataSourceCode, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query("UPDATE JobDataSourceCompanyEntity c SET c.deleted = true, c.updatedTime = :deletedAt WHERE c.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("deletedAt") Instant deletedAt);

    // 保留原有的物理删除方法，供特殊情况使用
    @Modifying
    @Query("DELETE FROM JobDataSourceCompanyEntity c WHERE c.dataSourceCode = :dataSourceCode")
    void hardDeleteByDataSourceCode(@Param("dataSourceCode") String dataSourceCode);
}