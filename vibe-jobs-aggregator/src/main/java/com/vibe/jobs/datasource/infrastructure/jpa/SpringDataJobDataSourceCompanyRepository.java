package com.vibe.jobs.datasource.infrastructure.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataJobDataSourceCompanyRepository extends JpaRepository<JobDataSourceCompanyEntity, Long> {
    
    // 由于 deleted 字段现在是 NOT NULL 且默认为 false，可以简化查询条件
    @Query("SELECT c FROM JobDataSourceCompanyEntity c WHERE c.dataSourceCode = :dataSourceCode AND c.deleted = false ORDER BY c.reference, c.id DESC")
    List<JobDataSourceCompanyEntity> findByDataSourceCodeOrderByReference(@Param("dataSourceCode") String dataSourceCode);
    
    @Query("SELECT c FROM JobDataSourceCompanyEntity c WHERE c.dataSourceCode = :dataSourceCode AND c.deleted = false ORDER BY c.reference, c.id DESC")
    Page<JobDataSourceCompanyEntity> findByDataSourceCodeOrderByReference(@Param("dataSourceCode") String dataSourceCode, Pageable pageable);
    
    // 检查指定数据源和引用的活跃公司是否存在（用于唯一性检查）
    @Query("SELECT c FROM JobDataSourceCompanyEntity c WHERE c.dataSourceCode = :dataSourceCode AND c.reference = :reference AND c.deleted = false")
    Optional<JobDataSourceCompanyEntity> findActiveByDataSourceCodeAndReference(@Param("dataSourceCode") String dataSourceCode, @Param("reference") String reference);
    
    // 检查指定数据源和引用的活跃公司是否存在，排除特定ID（用于更新时的唯一性检查）
    @Query("SELECT c FROM JobDataSourceCompanyEntity c WHERE c.dataSourceCode = :dataSourceCode AND c.reference = :reference AND c.deleted = false AND c.id != :excludeId")
    Optional<JobDataSourceCompanyEntity> findActiveByDataSourceCodeAndReferenceExcludingId(@Param("dataSourceCode") String dataSourceCode, @Param("reference") String reference, @Param("excludeId") Long excludeId);
    
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
