package com.vibe.jobs.companydiscovery.domain.spi;

import com.vibe.jobs.companydiscovery.domain.CompanyDiscoveryResult;

import java.util.List;

public interface CompanyDiscoveryResultRepositoryPort {

    CompanyDiscoveryResult save(CompanyDiscoveryResult result);

    List<CompanyDiscoveryResult> fetchRecent(int limit);
}
