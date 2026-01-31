package com.vibe.jobs.companydiscovery.domain.spi;

import com.vibe.jobs.companydiscovery.domain.CompanyCandidate;

import java.util.List;

public interface CompanyDiscoveryProviderPort {

    String providerName();

    boolean supports(String dataSourceType);

    List<CompanyCandidate> discover(CompanyDiscoveryRequest request);

    record CompanyDiscoveryRequest(String dataSourceType,
                                   int maxCandidates,
                                   String baseUrl,
                                   List<String> seedCompanies) {
    }
}
