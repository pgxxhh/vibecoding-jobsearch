package com.vibe.jobs.admin.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vibe.jobs.admin.domain.IngestionSettingsSnapshot;
import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;

import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IngestionSettingsRequest(
        long fixedDelayMs,
        long initialDelayMs,
        int pageSize,
        int recentDays,
        int concurrency,
        IngestionProperties.LocationFilter locationFilter,
        IngestionProperties.RoleFilter roleFilter,
        Map<String, IngestionProperties.CompanyOverride> companyOverrides
) {
    public IngestionSettingsSnapshot toSnapshot() {
        return new IngestionSettingsSnapshot(
                fixedDelayMs,
                initialDelayMs,
                pageSize,
                recentDays,
                Math.max(1, concurrency), // 确保并发数至少为1
                companyOverrides != null ? companyOverrides : Map.of(),
                locationFilter,
                roleFilter,
                Instant.now()
        );
    }
}
