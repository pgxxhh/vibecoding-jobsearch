package com.vibe.jobs.admin.interfaces.dto;

import com.vibe.jobs.companydiscovery.domain.CompanyDiscoveryRun;

import java.time.Instant;

public record CompanyDiscoveryRunResponse(
        Long id,
        String status,
        String provider,
        boolean dryRun,
        int totalCandidates,
        int totalValid,
        Instant startedAt,
        Instant completedAt
) {

    public static CompanyDiscoveryRunResponse fromDomain(CompanyDiscoveryRun run) {
        return new CompanyDiscoveryRunResponse(
                run.id(),
                run.status().name(),
                run.provider(),
                run.dryRun(),
                run.totalCandidates(),
                run.totalValid(),
                run.startedAt(),
                run.completedAt()
        );
    }
}
