package com.vibe.jobs.ingestion;

import com.vibe.jobs.config.IngestionProperties;
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

    public JobIngestionFilter(IngestionProperties properties) {
        this.properties = properties;
    }

    public List<FetchedJob> apply(List<FetchedJob> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return List.of();
        }

        IngestionProperties.Mode mode = properties.getMode();
        if (mode == IngestionProperties.Mode.COMPANIES) {
            Set<String> allowedCompanies = new HashSet<>(properties.normalizedCompanies());
            if (allowedCompanies.isEmpty()) {
                return jobs; // nothing to filter against
            }
            return jobs.stream()
                    .filter(job -> matchesCompany(job.job(), allowedCompanies))
                    .toList();
        }

        if (mode == IngestionProperties.Mode.RECENT) {
            Instant cutoff = Instant.now().minus(Duration.ofDays(Math.max(properties.getRecentDays(), 1)));
            return jobs.stream()
                    .filter(job -> isRecent(job.job(), cutoff))
                    .toList();
        }

        return jobs;
    }

    private boolean matchesCompany(Job job, Set<String> allowed) {
        if (job == null) return false;
        String company = job.getCompany();
        if (company == null) return false;
        return allowed.contains(company.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isRecent(Job job, Instant cutoff) {
        if (job == null) return false;
        Instant postedAt = job.getPostedAt();
        if (postedAt == null) {
            return true; // keep if timestamp unknown
        }
        return !postedAt.isBefore(cutoff);
    }
}
