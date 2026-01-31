package com.vibe.jobs.companydiscovery.domain.spi;

import com.vibe.jobs.companydiscovery.domain.CompanyDiscoveryRun;

import java.util.List;

public interface CompanyDiscoveryRunRepositoryPort {

    CompanyDiscoveryRun save(CompanyDiscoveryRun run);

    List<CompanyDiscoveryRun> fetchRecent(int limit);
}
