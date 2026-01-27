package com.vibe.jobs.jobposting.application;

import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;
import com.vibe.jobs.ingestion.infrastructure.sourceclient.FetchedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleFilterService {

    private static final Logger log = LoggerFactory.getLogger(RoleFilterService.class);

    private final IngestionProperties properties;

    public RoleFilterService(IngestionProperties properties) {
        this.properties = properties;
    }

    public List<FetchedJob> filter(List<FetchedJob> jobs) {
        IngestionProperties.RoleFilter filter = properties.getRoleFilter();
        if (!filter.isEnabled() || jobs == null || jobs.isEmpty()) {
            return jobs;
        }

        List<FetchedJob> filtered = jobs.stream()
                .filter(job -> JobFilterMatcher.matchesRole(job, filter))
                .collect(Collectors.toList());

        if (jobs.size() != filtered.size()) {
            log.info("Role filter: {} jobs -> {} jobs (filtered out {})", jobs.size(), filtered.size(), jobs.size() - filtered.size());
        }

        return filtered;
    }

    public String getFilterStatus() {
        IngestionProperties.RoleFilter filter = properties.getRoleFilter();
        if (!filter.isEnabled()) {
            return "Role filter: DISABLED";
        }
        StringBuilder status = new StringBuilder("Role filter: ENABLED\n");
        if (!filter.getIncludeKeywords().isEmpty()) {
            status.append("  Include keywords: ").append(filter.getIncludeKeywords()).append('\n');
        }
        if (!filter.getExcludeKeywords().isEmpty()) {
            status.append("  Exclude keywords: ").append(filter.getExcludeKeywords()).append('\n');
        }
        status.append("  Search description: ").append(filter.isSearchDescription());
        return status.toString().trim();
    }

}
