package com.vibe.jobs.service.enrichment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;

@Component
public class JobContentEnrichmentClient {

    private static final Logger log = LoggerFactory.getLogger(JobContentEnrichmentClient.class);

    private final boolean enabled;
    private final String providerKey;
    private final Map<String, JobContentEnrichmentProvider> providersByName;

    public JobContentEnrichmentClient(@Value("${jobs.detail-enhancement.enabled:true}") boolean enabled,
                                      @Value("${jobs.detail-enhancement.provider:}") String provider,
                                      List<JobContentEnrichmentProvider> providers) {
        this.enabled = enabled;
        this.providerKey = normalize(provider);
        this.providersByName = buildProviderMap(providers);
    }

    public JobContentEnrichmentResult enrich(JobSnapshot job, String rawContent, String contentText, String fingerprint) {
        if (!enabled) {
            return JobContentEnrichmentResult.failure(null, fingerprint, "CLIENT_DISABLED", "Enrichment disabled");
        }
        if (job == null || job.id() == null) {
            return JobContentEnrichmentResult.failure(null, fingerprint, "INVALID_JOB", "Job is required");
        }
        JobContentEnrichmentProvider provider = resolveProvider();
        if (provider == null) {
            return JobContentEnrichmentResult.failure(null, fingerprint, "NO_PROVIDER", "No provider available");
        }
        JobContentEnrichmentResult result = provider.enrich(job, rawContent, contentText, fingerprint);
        if (result == null) {
            return JobContentEnrichmentResult.failure(provider.name(), fingerprint, "EMPTY_RESULT", "Provider returned null result");
        }
        if (result.sourceFingerprint() == null) {
            return result.withFingerprint(fingerprint);
        }
        return result;
    }

    private JobContentEnrichmentProvider resolveProvider() {
        if (providersByName.isEmpty()) {
            log.info("No job content enrichment providers registered");
            return null;
        }
        if (providerKey != null) {
            JobContentEnrichmentProvider configured = providersByName.get(providerKey);
            if (configured != null) {
                if (configured.isEnabled()) {
                    return configured;
                }
                log.warn("Job content enrichment provider '{}' is configured but currently disabled", providerKey);
            } else {
                log.warn("Unknown job content enrichment provider '{}' configured", providerKey);
            }
        }
        return providersByName.values().stream()
                .filter(JobContentEnrichmentProvider::isEnabled)
                .findFirst()
                .orElseGet(() -> {
                    log.info("All job content enrichment providers are disabled");
                    return null;
                });
    }

    private Map<String, JobContentEnrichmentProvider> buildProviderMap(List<JobContentEnrichmentProvider> providers) {
        Map<String, JobContentEnrichmentProvider> map = new LinkedHashMap<>();
        if (providers == null) {
            return map;
        }
        for (JobContentEnrichmentProvider provider : providers) {
            if (provider == null) {
                continue;
            }
            String key = normalize(provider.name());
            if (key == null) {
                continue;
            }
            map.putIfAbsent(key, provider);
        }
        return map;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
