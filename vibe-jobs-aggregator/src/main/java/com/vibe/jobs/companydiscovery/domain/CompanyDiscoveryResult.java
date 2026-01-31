package com.vibe.jobs.companydiscovery.domain;

import java.time.Instant;

public record CompanyDiscoveryResult(Long id,
                                     Long runId,
                                     String dataSourceCode,
                                     String companyReference,
                                     String displayName,
                                     String provider,
                                     CompanyDiscoveryResultStatus status,
                                     String reason,
                                     Instant createdAt) {
}
