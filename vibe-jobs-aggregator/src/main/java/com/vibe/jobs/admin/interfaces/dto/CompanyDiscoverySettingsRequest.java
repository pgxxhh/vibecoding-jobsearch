package com.vibe.jobs.admin.interfaces.dto;

import com.vibe.jobs.admin.domain.CompanyDiscoverySettingsSnapshot;
import com.vibe.jobs.shared.infrastructure.config.CompanyDiscoveryProperties;
import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CompanyDiscoverySettingsRequest(
        boolean enabled,
        long fixedDelayMs,
        long initialDelayMs,
        int pageSize,
        int maxCandidatesPerRun,
        boolean dryRun,
        List<String> includeDataSourceTypes,
        List<String> excludeCompanies,
        Map<String, CompanyDiscoveryProperties.ProviderSettings> providers,
        IngestionProperties.LocationFilter locationFilter,
        IngestionProperties.RoleFilter roleFilter
) {

    public CompanyDiscoverySettingsSnapshot toSnapshot() {
        return new CompanyDiscoverySettingsSnapshot(
                enabled,
                fixedDelayMs,
                initialDelayMs,
                pageSize,
                maxCandidatesPerRun,
                dryRun,
                includeDataSourceTypes,
                excludeCompanies,
                providers,
                locationFilter,
                roleFilter,
                Instant.now()
        );
    }
}
