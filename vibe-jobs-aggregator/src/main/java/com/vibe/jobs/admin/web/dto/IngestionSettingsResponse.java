package com.vibe.jobs.admin.web.dto;

import com.vibe.jobs.admin.domain.IngestionSettingsSnapshot;
import com.vibe.jobs.config.IngestionProperties;

import java.time.Instant;
import java.util.Map;

public record IngestionSettingsResponse(
        long fixedDelayMs,
        long initialDelayMs,
        int pageSize,
        IngestionProperties.Mode mode,
        int recentDays,
        int concurrency,
        Map<String, IngestionProperties.CompanyOverride> companyOverrides,
        IngestionProperties.LocationFilter locationFilter,
        IngestionProperties.RoleFilter roleFilter,
        Instant updatedAt
) {
    public static IngestionSettingsResponse fromSnapshot(IngestionSettingsSnapshot snapshot) {
        return new IngestionSettingsResponse(
                snapshot.fixedDelayMs(),
                snapshot.initialDelayMs(),
                snapshot.pageSize(),
                snapshot.mode(),
                snapshot.recentDays(),
                snapshot.concurrency(),
                snapshot.companyOverrides(),
                snapshot.locationFilter(),
                snapshot.roleFilter(),
                snapshot.updatedAt()
        );
    }
}
