package com.vibe.jobs.datasource.application;

import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSourceRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    public List<JobDataSource> fetchAll() {
        return repository.findAll();
    }

    public JobDataSource getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + id));
    }

    public JobDataSource getByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + code));
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

    public Map<String, Set<String>> getNormalizedCompaniesBySource() {
        Map<String, Set<String>> mapping = new HashMap<>();
        for (JobDataSource source : repository.findAllEnabled()) {
            if (source.getCompanies().isEmpty()) {
                continue;
            }
            Set<String> companies = new HashSet<>();
            for (JobDataSource.DataSourceCompany company : source.getCompanies()) {
                if (company == null || !company.enabled()) {
                    continue;
                }
                String name = company.displayName().isBlank() ? company.reference() : company.displayName();
                if (name == null || name.isBlank()) {
                    continue;
                }
                companies.add(name.trim().toLowerCase());
            }
            if (!companies.isEmpty()) {
                mapping.put(source.getCode().trim().toLowerCase(), companies);
            }
        }
        return mapping;
    }
}
