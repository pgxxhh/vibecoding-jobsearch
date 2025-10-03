package com.vibe.jobs.datasource.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobDataSourceRepository {

    List<JobDataSource> findAllEnabled();

    List<JobDataSource> findAll();

    Optional<JobDataSource> findById(Long id);

    JobDataSource save(JobDataSource dataSource);

    void saveAll(Collection<JobDataSource> sources);

    boolean existsAny();

    boolean existsByCode(String code);

    Optional<JobDataSource> findByCode(String code);
}
