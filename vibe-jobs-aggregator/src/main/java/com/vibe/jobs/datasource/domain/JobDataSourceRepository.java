package com.vibe.jobs.datasource.domain;

import java.util.List;
import java.util.Optional;

public interface JobDataSourceRepository {

    List<JobDataSource> findAllEnabled();

    List<JobDataSource> findAll();

    Optional<JobDataSource> findById(Long id);

    JobDataSource save(JobDataSource dataSource);

    void deleteById(Long id);
    
    /**
     * 软删除数据源
     */
    void delete(Long id);
    
    Optional<JobDataSource> findByCode(String code);
    
    /**
     * 查找包括软删除的记录
     */
    Optional<JobDataSource> findByIdIncludingDeleted(Long id);
    
    Optional<JobDataSource> findByCodeIncludingDeleted(String code);
}
