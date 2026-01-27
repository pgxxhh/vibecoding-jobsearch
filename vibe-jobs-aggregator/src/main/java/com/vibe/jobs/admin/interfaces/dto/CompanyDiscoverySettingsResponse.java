package com.vibe.jobs.admin.interfaces.dto;

import com.vibe.jobs.admin.domain.CompanyDiscoverySettingsSnapshot;
import com.vibe.jobs.shared.infrastructure.config.CompanyDiscoveryProperties;
import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CompanyDiscoverySettingsResponse(
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
        IngestionProperties.RoleFilter roleFilter,
        Instant updatedAt
) {

    public static CompanyDiscoverySettingsResponse fromSnapshot(CompanyDiscoverySettingsSnapshot snapshot) {
        return new CompanyDiscoverySettingsResponse(
                snapshot.enabled(),
                snapshot.fixedDelayMs(),
                snapshot.initialDelayMs(),
                snapshot.pageSize(),
                snapshot.maxCandidatesPerRun(),
                snapshot.dryRun(),
                snapshot.includeDataSourceTypes(),
                snapshot.excludeCompanies(),
                snapshot.providers(),
                snapshot.locationFilter(),
                snapshot.roleFilter(),
                snapshot.updatedAt()
        );
    }
}
