package com.vibe.jobs.datasource.application;

import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSourceRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class DataSourceCommandService {

    private final JobDataSourceRepository repository;

    public DataSourceCommandService(JobDataSourceRepository repository) {
        this.repository = repository;
    }

    public JobDataSource save(JobDataSource source) {
        return repository.save(source);
    }

    public void saveAll(Collection<JobDataSource> sources) {
        repository.saveAll(sources);
    }
}
