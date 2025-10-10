package com.vibe.jobs.ingestion;

import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.datasource.application.DataSourceQueryService;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.sources.FetchedJob;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class JobIngestionFilter {

    private final IngestionProperties properties;
    private final DataSourceQueryService queryService;

    public JobIngestionFilter(IngestionProperties properties, DataSourceQueryService queryService) {
        this.properties = properties;
        this.queryService = queryService;
    }

    public List<FetchedJob> apply(List<FetchedJob> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return List.of();
        }

        // 1. 首先按启用的公司过滤 (必须条件)
        Set<String> enabledCompanies = new HashSet<>(queryService.getNormalizedCompanyNames());
        
        // 2. 按时间范围过滤 (必须条件)
        Instant cutoff = Instant.now().minus(Duration.ofDays(Math.max(properties.getRecentDays(), 1)));
        
        return jobs.stream()
                .filter(job -> matchesEnabledCompany(job.job(), enabledCompanies))
                .filter(job -> isRecent(job.job(), cutoff))
                .toList();
    }

    private boolean matchesEnabledCompany(Job job, Set<String> enabledCompanies) {
        if (job == null) return false;
        String company = job.getCompany();
        if (company == null) return false;
        
        // 如果没有启用的公司，则跳过公司过滤
        if (enabledCompanies.isEmpty()) {
            return true;
        }
        
        return enabledCompanies.contains(company.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isRecent(Job job, Instant cutoff) {
        if (job == null) return false;
        Instant postedAt = job.getPostedAt();
        if (postedAt == null) {
            return true; // 如果时间戳未知，保留职位
        }
        return !postedAt.isBefore(cutoff);
    }
}
