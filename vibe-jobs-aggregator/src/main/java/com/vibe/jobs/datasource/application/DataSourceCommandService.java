package com.vibe.jobs.datasource.application;

import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DataSourceCommandService {

    private final JobDataSourceRepository repository;

    public DataSourceCommandService(JobDataSourceRepository repository) {
        this.repository = repository;
    }

    public JobDataSource save(JobDataSource source) {
        return repository.save(source);
    }

    /**
     * 软删除数据源
     */
    @Transactional
    public void delete(Long id) {
        repository.delete(id);
    }

    /**
     * 物理删除数据源（仅供特殊情况使用）
     */
    @Transactional
    public void hardDelete(Long id) {
        repository.deleteById(id);
    }
}
