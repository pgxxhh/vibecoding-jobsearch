package com.vibe.jobs.companydiscovery.infrastructure.provider;

import com.vibe.jobs.companydiscovery.domain.CompanyCandidate;
import com.vibe.jobs.companydiscovery.domain.spi.CompanyDiscoveryProviderPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SeedCompanyDiscoveryAdapter implements CompanyDiscoveryProviderPort {

    @Override
    public String providerName() {
        return "seed";
    }

    @Override
    public boolean supports(String dataSourceType) {
        return dataSourceType != null && !dataSourceType.isBlank();
    }

    @Override
    public List<CompanyCandidate> discover(CompanyDiscoveryRequest request) {
        if (request.seedCompanies() == null || request.seedCompanies().isEmpty()) {
            return List.of();
        }
        return request.seedCompanies().stream()
                .filter(seed -> seed != null && !seed.isBlank())
                .map(seed -> new CompanyCandidate(seed, seed, seed, Map.of(), Map.of(), providerName()))
                .toList();
    }
}
