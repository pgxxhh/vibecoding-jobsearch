package com.vibe.jobs.service.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class JobContentEnrichmentClient {

    private static final Logger log = LoggerFactory.getLogger(JobContentEnrichmentClient.class);

    private final WebClient webClient;
    private final boolean enabled;
    private final Duration timeout;
    private final ObjectMapper objectMapper;
    private final String endpoint;

    public JobContentEnrichmentClient(ObjectMapper objectMapper,
                                      @Value("${jobs.detail-enhancement.enabled:true}") boolean enhancementEnabled,
                                      @Value("${jobs.detail-enhancement.enrichment-service.base-url:}") String baseUrl,
                                      @Value("${jobs.detail-enhancement.enrichment-service.api-key:}") String apiKey,
                                      @Value("${jobs.detail-enhancement.enrichment-service.endpoint:/v1/enrich}") String endpoint,
                                      @Value("${jobs.detail-enhancement.enrichment-service.timeout:PT15S}") Duration timeout) {
        this.objectMapper = objectMapper;
        this.timeout = timeout;
        this.endpoint = endpoint;
        this.enabled = enhancementEnabled && StringUtils.hasText(baseUrl);
        if (this.enabled) {
            WebClient.Builder builder = WebClient.builder()
                    .baseUrl(baseUrl);
            if (StringUtils.hasText(apiKey)) {
                builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            }
            this.webClient = builder.build();
        } else {
            this.webClient = null;
        }
    }

    public Optional<JobContentEnrichment> enrich(Job job, String rawContent, String contentText) {
        if (!enabled || webClient == null || job == null) {
            return Optional.empty();
        }
        try {
            EnrichmentRequest request = new EnrichmentRequest(
                    job.getId(),
                    job.getTitle(),
                    job.getCompany(),
                    job.getLocation(),
                    rawContent,
                    contentText
            );
            EnrichmentResponse response = webClient.post()
                    .uri(endpoint)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EnrichmentResponse.class)
                    .timeout(timeout)
                    .block();
            if (response == null) {
                return Optional.empty();
            }
            String structuredJson = serializeStructured(response.structured());
            return Optional.of(new JobContentEnrichment(
                    normalize(response.summary()),
                    response.skills(),
                    response.highlights(),
                    structuredJson
            ));
        } catch (WebClientResponseException ex) {
            log.warn("Failed to enrich job {} due to HTTP {}: {}", job.getId(), ex.getStatusCode(), ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Failed to enrich job {}: {}", job.getId(), ex.getMessage());
            return Optional.empty();
        }
    }

    private String serializeStructured(Map<String, Object> structured) {
        if (structured == null || structured.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(structured);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize structured enrichment payload: {}", ex.getMessage());
            return null;
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record EnrichmentRequest(Long jobId,
                                     String title,
                                     String company,
                                     String location,
                                     String content,
                                     String contentText) {
    }

    private record EnrichmentResponse(String summary,
                                      List<String> skills,
                                      List<String> highlights,
                                      Map<String, Object> structured) {
    }
}
