package com.vibe.jobs.companydiscovery.application;

import com.vibe.jobs.admin.domain.CompanyDiscoverySettingsSnapshot;
import com.vibe.jobs.companydiscovery.domain.CompanyCandidate;
import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.ingestion.infrastructure.sourceclient.FetchedJob;
import com.vibe.jobs.ingestion.infrastructure.sourceclient.SourceClient;
import com.vibe.jobs.ingestion.infrastructure.sourceclient.SourceClientFactory;
import com.vibe.jobs.jobposting.application.JobFilterMatcher;
import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CompanyDiscoveryValidationService {

    private static final Logger log = LoggerFactory.getLogger(CompanyDiscoveryValidationService.class);

    private final SourceClientFactory sourceClientFactory;
    private final CompanyDiscoveryOptionResolver optionResolver;

    public CompanyDiscoveryValidationService(SourceClientFactory sourceClientFactory,
                                             CompanyDiscoveryOptionResolver optionResolver) {
        this.sourceClientFactory = sourceClientFactory;
        this.optionResolver = optionResolver;
    }

    public ValidationOutcome validate(JobDataSource dataSource,
                                      CompanyCandidate candidate,
                                      CompanyDiscoverySettingsSnapshot settings) {
        JobDataSource.DataSourceCompany company = new JobDataSource.DataSourceCompany(
                null,
                candidate.reference(),
                candidate.displayName(),
                candidate.slug(),
                true,
                candidate.placeholderOverrides(),
                candidate.overrideOptions()
        );

        Map<String, String> options = optionResolver.resolve(dataSource, company);
        if (options.isEmpty()) {
            return ValidationOutcome.invalid("No valid options derived for candidate");
        }

        try {
            SourceClient client = sourceClientFactory.create(dataSource.getType(), options);
            List<FetchedJob> jobs = client.fetchPage(1, settings.pageSize());
            if (jobs == null || jobs.isEmpty()) {
                return ValidationOutcome.invalid("No jobs returned from source");
            }

            IngestionProperties.LocationFilter locationFilter = settings.locationFilter();
            IngestionProperties.RoleFilter roleFilter = settings.roleFilter();

            long matched = jobs.stream()
                    .filter(job -> JobFilterMatcher.matchesLocation(job.job().getLocation(), locationFilter))
                    .filter(job -> JobFilterMatcher.matchesRole(job, roleFilter))
                    .count();

            if (matched <= 0) {
                return ValidationOutcome.invalid("No jobs matched location/role filters");
            }

            return ValidationOutcome.valid((int) matched);
        } catch (Exception ex) {
            log.warn("Validation failed for candidate {} in data source {}: {}", candidate.reference(), dataSource.getCode(), ex.getMessage());
            return ValidationOutcome.invalid(ex.getMessage() == null ? "Validation failed" : ex.getMessage());
        }
    }

    public record ValidationOutcome(boolean valid, int matchedJobs, String reason) {
        public static ValidationOutcome valid(int matchedJobs) {
            return new ValidationOutcome(true, matchedJobs, null);
        }

        public static ValidationOutcome invalid(String reason) {
            return new ValidationOutcome(false, 0, reason);
        }
    }
}
