package com.vibe.jobs.companydiscovery.infrastructure.provider;

import com.vibe.jobs.companydiscovery.domain.CompanyCandidate;
import com.vibe.jobs.companydiscovery.domain.spi.CompanyDiscoveryProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SmartRecruitersCompanyDiscoveryAdapter implements CompanyDiscoveryProviderPort {

    private static final Logger log = LoggerFactory.getLogger(SmartRecruitersCompanyDiscoveryAdapter.class);
    private static final String DEFAULT_BASE_URL = "https://api.smartrecruiters.com/v1/companies";
    private static final int DEFAULT_PAGE_LIMIT = 100;

    private final WebClient.Builder webClientBuilder;

    public SmartRecruitersCompanyDiscoveryAdapter(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public String providerName() {
        return "smartrecruiters";
    }

    @Override
    public boolean supports(String dataSourceType) {
        return "smartrecruiters".equalsIgnoreCase(dataSourceType);
    }

    @Override
    public List<CompanyCandidate> discover(CompanyDiscoveryRequest request) {
        String baseUrl = request.baseUrl() == null || request.baseUrl().isBlank()
                ? DEFAULT_BASE_URL
                : request.baseUrl().trim();
        int maxCandidates = Math.max(request.maxCandidates(), 1);
        List<CompanyCandidate> candidates = new ArrayList<>();
        int offset = 0;

        while (candidates.size() < maxCandidates) {
            int limit = Math.min(DEFAULT_PAGE_LIMIT, maxCandidates - candidates.size());
            Map<String, Object> response = fetchCompanies(baseUrl, limit, offset);
            List<Map<String, Object>> items = extractCompanyList(response);
            if (items.isEmpty()) {
                break;
            }

            for (Map<String, Object> item : items) {
                CompanyCandidate candidate = toCandidate(item);
                if (candidate == null || candidate.reference().isBlank()) {
                    continue;
                }
                candidates.add(candidate);
                if (candidates.size() >= maxCandidates) {
                    break;
                }
            }

            if (items.size() < limit) {
                break;
            }
            offset += limit;
        }

        log.info("SmartRecruiters discovery returned {} candidates", candidates.size());
        return candidates;
    }

    private Map<String, Object> fetchCompanies(String baseUrl, int limit, int offset) {
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractCompanyList(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return List.of();
        }
        Object content = response.get("content");
        if (content instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        Object companies = response.get("companies");
        if (companies instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private CompanyCandidate toCandidate(Map<String, Object> item) {
        if (item == null) {
            return null;
        }
        String identifier = value(item.get("identifier"));
        if (identifier.isBlank()) {
            identifier = value(item.get("id"));
        }
        String name = value(item.get("name"));
        return new CompanyCandidate(identifier, name, identifier, Map.of(), Map.of(), providerName());
    }

    private String value(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
