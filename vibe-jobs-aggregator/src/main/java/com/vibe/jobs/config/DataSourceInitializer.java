package com.vibe.jobs.config;

import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class DataSourceInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSourceInitializer.class);

    private final JobDataSourceRepository repository;

    public DataSourceInitializer(JobDataSourceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Initializing default data sources...");
        
        // 确保默认的数据源存在并设置为启用状态
        ensureDataSource("greenhouse", "greenhouse", true, true);
        ensureDataSource("lever", "lever", true, true);
        ensureDataSource("workday", "workday", true, true);
        ensureDataSource("ashby", "ashby", true, true);
        ensureDataSource("smartrecruiters", "smartrecruiters", true, true);
        ensureDataSource("recruitee", "recruitee", true, true);
        ensureDataSource("workable", "workable", true, true);
        
        log.info("Data source initialization completed");
    }

    private void ensureDataSource(String code, String type, boolean enabled, boolean runOnStartup) {
        if (repository.findByCode(code).isEmpty()) {
            log.info("Creating data source: {}", code);
            JobDataSource dataSource = new JobDataSource(
                    null, // id will be auto-generated
                    code,
                    type,
                    enabled,
                    runOnStartup,
                    false, // requireOverride
                    JobDataSource.Flow.UNLIMITED,
                    Map.of(), // baseOptions
                    List.of(), // categories
                    List.of() // companies
            );
            repository.save(dataSource);
        } else {
            // 如果数据源存在但被禁用，重新启用它
            JobDataSource existing = repository.findByCode(code).get();
            if (!existing.isEnabled() && enabled) {
                log.info("Re-enabling data source: {}", code);
                JobDataSource updated = new JobDataSource(
                        existing.getId(),
                        existing.getCode(),
                        existing.getType(),
                        true, // enabled
                        true, // runOnStartup
                        existing.isRequireOverride(),
                        existing.getFlow(),
                        existing.getBaseOptions(),
                        existing.getCategories(),
                        existing.getCompanies()
                );
                repository.save(updated);
            }
        }
    }
}