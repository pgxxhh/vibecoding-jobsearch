package com.vibe.jobs.service.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DeepSeekJobContentEnrichmentProvider implements JobContentEnrichmentProvider {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekJobContentEnrichmentProvider.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String providerName;
    private final Duration timeout;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final String path;
    private final boolean enabled;
    private final Map<String, Object> responseFormat;

    public DeepSeekJobContentEnrichmentProvider(ObjectMapper objectMapper,
                                                @Value("${jobs.detail-enhancement.deepseek.api-key:}") String apiKey,
                                                @Value("${jobs.detail-enhancement.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
                                                @Value("${jobs.detail-enhancement.deepseek.path:/chat/completions}") String path,
                                                @Value("${jobs.detail-enhancement.deepseek.model:deepseek-chat}") String model,
                                                @Value("${jobs.detail-enhancement.deepseek.timeout:PT20S}") Duration timeout,
                                                @Value("${jobs.detail-enhancement.deepseek.temperature:0.2}") double temperature,
                                                @Value("${jobs.detail-enhancement.deepseek.max-output-tokens:800}") int maxTokens) {
        this.objectMapper = objectMapper;
        this.timeout = timeout;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.path = path;
        this.providerName = "deepseek";
        this.enabled = StringUtils.hasText(apiKey);
        this.responseFormat = Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", JobContentEnrichmentSupport.schemaName(),
                        "schema", JobContentEnrichmentSupport.responseSchema()
                )
        );
        if (this.enabled) {
            this.webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
        } else {
            this.webClient = null;
        }
    }

    @Override
    public String name() {
        return providerName;
    }

    @Override
    public boolean isEnabled() {
        return enabled && webClient != null;
    }

    @Override
    public Optional<JobContentEnrichment> enrich(Job job, String rawContent, String contentText) {
        if (!isEnabled() || job == null) {
            return Optional.empty();
        }
        try {
            DeepSeekRequest request = buildRequest(job, rawContent, contentText);
            if (log.isDebugEnabled()) {
                try {
                    String payload = objectMapper.writeValueAsString(request);
                    log.debug("DeepSeek request payload for job {}: {}", job.getId(), payload);
                } catch (JsonProcessingException ignored) {
                    // ignore payload logging failures
                }
            }
            DeepSeekResponse response = webClient.post()
                    .uri(path)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(DeepSeekResponse.class)
                    .timeout(timeout)
                    .block();
            if (response == null) {
                return Optional.empty();
            }
            String content = response.firstMessageContent();
            if (!StringUtils.hasText(content)) {
                return Optional.empty();
            }
            ChatCompletionPayload payload = objectMapper.readValue(content, ChatCompletionPayload.class);
            String structuredJson = serializeStructured(payload.structured());
            List<String> skills = JobContentEnrichmentSupport.normalizeList(payload.skills());
            List<String> highlights = JobContentEnrichmentSupport.normalizeList(payload.highlights());
            return Optional.of(new JobContentEnrichment(
                    JobContentEnrichmentSupport.normalize(payload.summary()),
                    skills,
                    highlights,
                    structuredJson
            ));
        } catch (WebClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.warn("DeepSeek enrichment failed for job {} with HTTP {}: {}{}",
                    job.getId(), ex.getStatusCode(), ex.getMessage(),
                    StringUtils.hasText(responseBody) ? "; body=" + responseBody : "");
            return Optional.empty();
        } catch (JsonProcessingException ex) {
            log.warn("DeepSeek enrichment returned invalid JSON for job {}: {}", job.getId(), ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("DeepSeek enrichment failed for job {}: {}", job.getId(), ex.getMessage());
            return Optional.empty();
        }
    }

    private DeepSeekRequest buildRequest(Job job, String rawContent, String contentText) {
        String userPrompt = JobContentEnrichmentSupport.buildUserPrompt(job, rawContent, contentText);
        List<Message> messages = List.of(
                new Message("system", JobContentEnrichmentSupport.systemPrompt()),
                new Message("user", userPrompt)
        );
        return new DeepSeekRequest(model, messages, temperature, maxTokens, responseFormat);
    }

    private String serializeStructured(Map<String, Object> structured) throws JsonProcessingException {
        if (structured == null || structured.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(structured);
    }

    private record DeepSeekRequest(String model,
                                   List<Message> messages,
                                   Double temperature,
                                   Integer max_tokens,
                                   Map<String, Object> response_format) {
    }

    private record Message(String role, String content) {
    }

    private record DeepSeekResponse(List<Choice> choices) {
        String firstMessageContent() {
            if (choices == null) {
                return null;
            }
            return choices.stream()
                    .map(Choice::message)
                    .filter(message -> message != null && StringUtils.hasText(message.content()))
                    .map(Message::content)
                    .findFirst()
                    .orElse(null);
        }
    }

    private record Choice(Message message) {
    }

    private record ChatCompletionPayload(String summary,
                                         List<String> skills,
                                         List<String> highlights,
                                         Map<String, Object> structured) {
    }
}
