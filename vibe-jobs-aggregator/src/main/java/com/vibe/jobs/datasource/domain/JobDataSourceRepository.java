package com.vibe.jobs.datasource.domain;

import java.util.List;
import java.util.Optional;

public interface JobDataSourceRepository {

    List<JobDataSource> findAllEnabled();

    List<JobDataSource> findAll();

    Optional<JobDataSource> findById(Long id);

    JobDataSource save(JobDataSource dataSource);

    boolean existsByCode(String code);
}
