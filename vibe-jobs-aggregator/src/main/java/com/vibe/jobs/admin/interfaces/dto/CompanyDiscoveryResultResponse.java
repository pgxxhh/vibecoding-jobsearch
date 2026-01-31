package com.vibe.jobs.admin.interfaces.dto;

import com.vibe.jobs.companydiscovery.domain.CompanyDiscoveryResult;

import java.time.Instant;

public record CompanyDiscoveryResultResponse(
        Long id,
        Long runId,
        String dataSourceCode,
        String companyReference,
        String displayName,
        String provider,
        String status,
        String reason,
        Instant createdAt
) {

    public static CompanyDiscoveryResultResponse fromDomain(CompanyDiscoveryResult result) {
        return new CompanyDiscoveryResultResponse(
                result.id(),
                result.runId(),
                result.dataSourceCode(),
                result.companyReference(),
                result.displayName(),
                result.provider(),
                result.status().name(),
                result.reason(),
                result.createdAt()
        );
    }
}
