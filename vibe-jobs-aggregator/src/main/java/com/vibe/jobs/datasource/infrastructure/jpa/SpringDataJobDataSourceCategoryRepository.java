package com.vibe.jobs.datasource.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataJobDataSourceCategoryRepository extends JpaRepository<JobDataSourceCategoryEntity, Long> {
    
    List<JobDataSourceCategoryEntity> findByDataSourceCodeOrderByName(String dataSourceCode);
    
    void deleteByDataSourceCode(String dataSourceCode);
}