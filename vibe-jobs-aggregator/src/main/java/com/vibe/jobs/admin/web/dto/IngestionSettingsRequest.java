package com.vibe.jobs.admin.web.dto;

import com.vibe.jobs.admin.domain.IngestionSettingsSnapshot;
import com.vibe.jobs.config.IngestionProperties;

import java.time.Instant;
import java.util.Map;

public record IngestionSettingsRequest(
        long fixedDelayMs,
        long initialDelayMs,
        int pageSize,
        String mode,
        int recentDays,
        int concurrency,
        IngestionProperties.LocationFilter locationFilter,
        IngestionProperties.RoleFilter roleFilter,
        Map<String, IngestionProperties.CompanyOverride> companyOverrides
) {
    public IngestionSettingsSnapshot toSnapshot() {
        IngestionProperties.Mode resolvedMode;
        try {
            resolvedMode = mode == null ? IngestionProperties.Mode.RECENT : IngestionProperties.Mode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException ex) {
            resolvedMode = IngestionProperties.Mode.RECENT;
        }
        return new IngestionSettingsSnapshot(
                fixedDelayMs,
                initialDelayMs,
                pageSize,
                resolvedMode,
                recentDays,
                concurrency,
                companyOverrides,
                locationFilter,
                roleFilter,
                Instant.now()
        );
    }
}
