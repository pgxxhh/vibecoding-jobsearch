package com.vibe.jobs.datasource.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataJobDataSourceCompanyRepository extends JpaRepository<JobDataSourceCompanyEntity, Long> {
    
    List<JobDataSourceCompanyEntity> findByDataSourceCodeOrderByReference(String dataSourceCode);
    
    void deleteByDataSourceCode(String dataSourceCode);
}