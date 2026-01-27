package com.vibe.jobs.companydiscovery.domain;

import java.time.Instant;

public record CompanyDiscoveryRun(Long id,
                                  CompanyDiscoveryRunStatus status,
                                  String provider,
                                  boolean dryRun,
                                  int totalCandidates,
                                  int totalValid,
                                  Instant startedAt,
                                  Instant completedAt) {
}
