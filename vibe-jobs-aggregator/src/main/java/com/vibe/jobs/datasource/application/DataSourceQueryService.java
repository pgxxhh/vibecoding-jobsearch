package com.vibe.jobs.datasource.application;

import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class DataSourceQueryService {

    private final JobDataSourceRepository repository;

    public DataSourceQueryService(JobDataSourceRepository repository) {
        this.repository = repository;
    }

    public List<JobDataSource> fetchAllEnabled() {
        return repository.findAllEnabled();
    }

    public List<JobDataSource> fetchStartupSources() {
        return repository.findAllEnabled().stream()
                .filter(JobDataSource::isRunOnStartup)
                .collect(Collectors.toList());
    }

    public Set<String> getNormalizedCompanyNames() {
        Set<String> names = new TreeSet<>();
        for (JobDataSource source : repository.findAllEnabled()) {
            source.getCompanies().stream()
                    .filter(JobDataSource.DataSourceCompany::enabled)
                    .map(company -> company.displayName().isBlank() ? company.reference() : company.displayName())
                    .map(value -> value == null ? "" : value.trim().toLowerCase())
                    .filter(value -> !value.isBlank())
                    .forEach(names::add);
        }
        return names;
    }
}
