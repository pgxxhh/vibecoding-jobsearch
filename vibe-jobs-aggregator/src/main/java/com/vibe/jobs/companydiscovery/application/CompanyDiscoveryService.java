package com.vibe.jobs.companydiscovery.application;

import com.vibe.jobs.admin.application.AdminDataSourceService;
import com.vibe.jobs.admin.application.CompanyDiscoverySettingsService;
import com.vibe.jobs.admin.domain.CompanyDiscoverySettingsSnapshot;
import com.vibe.jobs.companydiscovery.domain.CompanyCandidate;
import com.vibe.jobs.companydiscovery.domain.CompanyDiscoveryResult;
import com.vibe.jobs.companydiscovery.domain.CompanyDiscoveryResultStatus;
import com.vibe.jobs.companydiscovery.domain.CompanyDiscoveryRun;
import com.vibe.jobs.companydiscovery.domain.CompanyDiscoveryRunStatus;
import com.vibe.jobs.companydiscovery.domain.spi.CompanyDiscoveryProviderPort;
import com.vibe.jobs.companydiscovery.domain.spi.CompanyDiscoveryResultRepositoryPort;
import com.vibe.jobs.companydiscovery.domain.spi.CompanyDiscoveryRunRepositoryPort;
import com.vibe.jobs.datasource.application.DataSourceQueryService;
import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.infrastructure.jpa.SpringDataJobDataSourceCompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CompanyDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(CompanyDiscoveryService.class);

    private final DataSourceQueryService dataSourceQueryService;
    private final CompanyDiscoverySettingsService settingsService;
    private final CompanyDiscoveryValidationService validationService;
    private final AdminDataSourceService adminDataSourceService;
    private final SpringDataJobDataSourceCompanyRepository companyRepository;
    private final CompanyDiscoveryRunRepositoryPort runRepository;
    private final CompanyDiscoveryResultRepositoryPort resultRepository;
    private final List<CompanyDiscoveryProviderPort> providers;

    public CompanyDiscoveryService(DataSourceQueryService dataSourceQueryService,
                                   CompanyDiscoverySettingsService settingsService,
                                   CompanyDiscoveryValidationService validationService,
                                   AdminDataSourceService adminDataSourceService,
                                   SpringDataJobDataSourceCompanyRepository companyRepository,
                                   CompanyDiscoveryRunRepositoryPort runRepository,
                                   CompanyDiscoveryResultRepositoryPort resultRepository,
                                   List<CompanyDiscoveryProviderPort> providers) {
        this.dataSourceQueryService = dataSourceQueryService;
        this.settingsService = settingsService;
        this.validationService = validationService;
        this.adminDataSourceService = adminDataSourceService;
        this.companyRepository = companyRepository;
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.providers = providers;
    }

    public CompanyDiscoveryRun runDiscovery(String triggeredBy) {
        CompanyDiscoverySettingsSnapshot settings = settingsService.current();
        if (!settings.enabled()) {
            log.info("Company discovery is disabled; skipping run");
            return null;
        }

        CompanyDiscoveryRun run = runRepository.save(new CompanyDiscoveryRun(
                null,
                CompanyDiscoveryRunStatus.RUNNING,
                "mixed",
                settings.dryRun(),
                0,
                0,
                Instant.now(),
                null
        ));

        int totalCandidates = 0;
        int totalValid = 0;

        try {
            for (JobDataSource source : resolveSources(settings)) {
                List<CompanyCandidate> discovered = discoverCandidates(settings, source.getType());
                for (CompanyCandidate candidate : discovered) {
                    totalCandidates++;
                    CompanyDiscoveryResultStatus status;
                    String reason = null;

                    if (isExcluded(settings, candidate)) {
                        status = CompanyDiscoveryResultStatus.INVALID;
                        reason = "Excluded by settings";
                    } else if (companyRepository.findActiveByDataSourceCodeAndReference(source.getCode(), candidate.reference()).isPresent()) {
                        status = CompanyDiscoveryResultStatus.SKIPPED_EXISTING;
                        reason = "Company already exists";
                    } else {
                        CompanyDiscoveryValidationService.ValidationOutcome outcome = validationService.validate(source, candidate, settings);
                        if (!outcome.valid()) {
                            status = CompanyDiscoveryResultStatus.INVALID;
                            reason = outcome.reason();
                        } else if (settings.dryRun()) {
                            status = CompanyDiscoveryResultStatus.DRY_RUN;
                            totalValid++;
                        } else {
                            try {
                                adminDataSourceService.createCompany(source.getCode(), toCompany(candidate));
                                status = CompanyDiscoveryResultStatus.CREATED;
                                totalValid++;
                            } catch (Exception ex) {
                                status = CompanyDiscoveryResultStatus.CREATE_FAILED;
                                reason = ex.getMessage();
                            }
                        }
                    }

                    resultRepository.save(new CompanyDiscoveryResult(
                            null,
                            run.id(),
                            source.getCode(),
                            candidate.reference(),
                            candidate.displayName(),
                            candidate.provider(),
                            status,
                            reason,
                            Instant.now()
                    ));
                }
            }

            CompanyDiscoveryRun completed = runRepository.save(new CompanyDiscoveryRun(
                    run.id(),
                    CompanyDiscoveryRunStatus.SUCCESS,
                    run.provider(),
                    settings.dryRun(),
                    totalCandidates,
                    totalValid,
                    run.startedAt(),
                    Instant.now()
            ));

            log.info("Company discovery run {} completed. candidates={}, valid={}", completed.id(), totalCandidates, totalValid);
            return completed;
        } catch (Exception ex) {
            CompanyDiscoveryRun failed = runRepository.save(new CompanyDiscoveryRun(
                    run.id(),
                    CompanyDiscoveryRunStatus.FAILED,
                    run.provider(),
                    settings.dryRun(),
                    totalCandidates,
                    totalValid,
                    run.startedAt(),
                    Instant.now()
            ));
            log.error("Company discovery run {} failed", failed.id(), ex);
            return failed;
        }
    }

    public List<CompanyDiscoveryRun> listRuns(int limit) {
        return runRepository.fetchRecent(limit);
    }

    public List<CompanyDiscoveryResult> listResults(int limit) {
        return resultRepository.fetchRecent(limit);
    }

    private List<JobDataSource> resolveSources(CompanyDiscoverySettingsSnapshot settings) {
        Set<String> allowedTypes = settings.includeDataSourceTypes().stream()
                .filter(type -> type != null && !type.isBlank())
                .map(type -> type.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return dataSourceQueryService.fetchAllEnabled().stream()
                .filter(Objects::nonNull)
                .filter(source -> source.getType() != null && !source.getType().isBlank())
                .filter(source -> !"crawler".equalsIgnoreCase(source.getType()))
                .filter(source -> allowedTypes.isEmpty() || allowedTypes.contains(source.getType().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private List<CompanyCandidate> discoverCandidates(CompanyDiscoverySettingsSnapshot settings, String dataSourceType) {
        List<CompanyCandidate> candidates = new ArrayList<>();
        Map<String, com.vibe.jobs.shared.infrastructure.config.CompanyDiscoveryProperties.ProviderSettings> providerSettings = settings.providers();

        for (CompanyDiscoveryProviderPort provider : providers) {
            String providerKey = provider.providerName().toLowerCase(Locale.ROOT);
            com.vibe.jobs.shared.infrastructure.config.CompanyDiscoveryProperties.ProviderSettings config = providerSettings.get(providerKey);
            if (config != null && !config.isEnabled()) {
                continue;
            }
            if (!provider.supports(dataSourceType)) {
                continue;
            }
            String baseUrl = config == null ? null : config.getBaseUrl();
            List<String> seedCompanies = config == null ? List.of() : config.getSeedCompanies();
            CompanyDiscoveryProviderPort.CompanyDiscoveryRequest request =
                    new CompanyDiscoveryProviderPort.CompanyDiscoveryRequest(
                            dataSourceType,
                            settings.maxCandidatesPerRun(),
                            baseUrl,
                            seedCompanies
                    );
            List<CompanyCandidate> discovered = provider.discover(request);
            candidates.addAll(discovered);
        }

        Set<String> seen = new java.util.HashSet<>();
        return candidates.stream()
                .filter(candidate -> candidate.reference() != null && !candidate.reference().isBlank())
                .filter(candidate -> seen.add(candidate.reference().trim().toLowerCase(Locale.ROOT)))
                .limit(settings.maxCandidatesPerRun())
                .toList();
    }

    private boolean isExcluded(CompanyDiscoverySettingsSnapshot settings, CompanyCandidate candidate) {
        if (settings.excludeCompanies().isEmpty()) {
            return false;
        }
        String normalized = candidate.reference().trim().toLowerCase(Locale.ROOT);
        return settings.excludeCompanies().stream().anyMatch(excluded -> excluded.equalsIgnoreCase(normalized));
    }

    private JobDataSource.DataSourceCompany toCompany(CompanyCandidate candidate) {
        return new JobDataSource.DataSourceCompany(
                null,
                candidate.reference(),
                candidate.displayName(),
                candidate.slug(),
                true,
                candidate.placeholderOverrides(),
                candidate.overrideOptions()
        );
    }
}
