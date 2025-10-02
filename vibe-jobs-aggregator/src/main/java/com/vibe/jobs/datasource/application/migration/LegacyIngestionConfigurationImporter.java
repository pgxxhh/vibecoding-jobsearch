package com.vibe.jobs.datasource.application.migration;

import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.datasource.application.DataSourceCommandService;
import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSource.CategoryQuotaDefinition;
import com.vibe.jobs.datasource.domain.JobDataSource.DataSourceCompany;
import com.vibe.jobs.datasource.domain.JobDataSource.Flow;
import com.vibe.jobs.datasource.domain.JobDataSourceRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "ingestion.migration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LegacyIngestionConfigurationImporter {

    private static final Logger log = LoggerFactory.getLogger(LegacyIngestionConfigurationImporter.class);

    private final IngestionProperties legacyProperties;
    private final DataSourceCommandService commandService;
    private final JobDataSourceRepository repository;

    public LegacyIngestionConfigurationImporter(IngestionProperties legacyProperties,
                                                DataSourceCommandService commandService,
                                                JobDataSourceRepository repository) {
        this.legacyProperties = legacyProperties;
        this.commandService = commandService;
        this.repository = repository;
    }

    @PostConstruct
    public void migrateIfNecessary() {
        if (repository.existsAny()) {
            log.info("Job data sources already initialized; skipping legacy ingestion migration");
            return;
        }
        if (legacyProperties.getSources() == null || legacyProperties.getSources().isEmpty()) {
            log.info("No legacy ingestion sources configured; skipping migration");
            return;
        }

        log.info("Migrating legacy ingestion configuration to database");
        List<JobDataSource> sources = new ArrayList<>();
        for (IngestionProperties.Source source : legacyProperties.getSources()) {
            if (source == null || source.getType() == null || source.getType().isBlank()) {
                continue;
            }
            JobDataSource dataSource = buildDataSource(source);
            if (dataSource == null) {
                continue;
            }
            sources.add(dataSource);
        }
        if (sources.isEmpty()) {
            log.info("No valid legacy data sources found; skipping migration");
            return;
        }
        commandService.saveAll(sources);
        log.info("Migrated {} legacy ingestion sources to the database", sources.size());
    }

    private JobDataSource buildDataSource(IngestionProperties.Source source) {
        Map<String, String> baseOptions = new LinkedHashMap<>(source.getOptions());
        List<CategoryQuotaDefinition> categories = new ArrayList<>();
        if (source.getCategories() != null) {
            for (IngestionProperties.Source.CategoryQuota quota : source.getCategories()) {
                if (quota == null || quota.getLimit() <= 0) {
                    continue;
                }
                categories.add(new CategoryQuotaDefinition(
                        quota.getName(),
                        quota.getLimit(),
                        quota.getTags(),
                        quota.getFacets()
                ));
            }
        }

        List<DataSourceCompany> companies = resolveCompaniesForSource(source);
        return new JobDataSource(
                null,
                normalizeCode(source),
                source.getType(),
                source.isEnabled(),
                source.isRunOnStartup(),
                source.isRequireOverride(),
                source.getFlow() == IngestionProperties.Source.Flow.LIMITED ? Flow.LIMITED : Flow.UNLIMITED,
                baseOptions,
                categories,
                companies
        ).normalized();
    }

    private List<DataSourceCompany> resolveCompaniesForSource(IngestionProperties.Source source) {
        List<DataSourceCompany> companies = new ArrayList<>();
        List<String> legacyCompanies = legacyProperties.getCompanies();
        if (legacyCompanies == null || legacyCompanies.isEmpty()) {
            return companies;
        }
        for (String companyName : legacyCompanies) {
            if (companyName == null || companyName.isBlank()) {
                continue;
            }
            String trimmed = companyName.trim();
            IngestionProperties.SourceOverride override = legacyProperties.getSourceOverride(trimmed, source.getType());
            if (override == null && source.isRequireOverride()) {
                continue;
            }
            boolean enabled = override == null || override.isEnabled();
            Map<String, String> overrideOptions = override == null ? Map.of() : override.optionsCopy();
            Map<String, String> placeholders = legacyProperties.getPlaceholderOverrides(trimmed);
            DataSourceCompany company = new DataSourceCompany(
                    null,
                    trimmed,
                    trimmed,
                    slugify(trimmed),
                    enabled,
                    placeholders,
                    overrideOptions
            );
            companies.add(company.normalized());
        }
        return companies;
    }

    private String normalizeCode(IngestionProperties.Source source) {
        if (source.getId() != null && !source.getId().isBlank()) {
            return source.getId().trim().toLowerCase(Locale.ROOT);
        }
        return source.getType().trim().toLowerCase(Locale.ROOT);
    }

    private String slugify(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
