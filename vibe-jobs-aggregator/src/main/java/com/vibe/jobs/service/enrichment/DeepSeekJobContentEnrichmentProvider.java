package com.vibe.jobs.service.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vibe.jobs.domain.JobEnrichmentKey;
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
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
        if (this.enabled) {
            log.info("apiKey: {}", apiKey);
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
    public JobContentEnrichmentResult enrich(JobSnapshot job, String rawContent, String contentText, String fingerprint) {
        if (!isEnabled() || job == null) {
            return JobContentEnrichmentResult.failure(providerName, fingerprint, "PROVIDER_DISABLED",
                    "Provider is disabled");
        }
        long start = System.nanoTime();
        try {
            DeepSeekRequest request = buildRequest(job, rawContent, contentText);
            if (log.isInfoEnabled()) {
                try {
                    String payload = objectMapper.writeValueAsString(request);
                    log.info("DeepSeek request payload for job {}: {}", job.id(), payload);
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
                return JobContentEnrichmentResult.failure(providerName, fingerprint, "EMPTY_RESPONSE",
                        "Provider returned empty response");
            }
            String content = response.firstMessageContent();
            if (!StringUtils.hasText(content)) {
                return JobContentEnrichmentResult.failure(providerName, fingerprint, "EMPTY_CONTENT",
                        "Provider returned empty content");
            }
            ChatCompletionPayload payload = objectMapper.readValue(content, ChatCompletionPayload.class);
            Map<JobEnrichmentKey, JsonNode> values = new EnumMap<>(JobEnrichmentKey.class);
            String normalizedSummary = JobContentEnrichmentSupport.normalize(payload.summary());
            if (StringUtils.hasText(normalizedSummary)) {
                values.put(JobEnrichmentKey.SUMMARY, objectMapper.valueToTree(normalizedSummary));
            }
            List<String> skills = JobContentEnrichmentSupport.normalizeList(payload.skills());
            values.put(JobEnrichmentKey.SKILLS, objectMapper.valueToTree(skills));
            List<String> highlights = JobContentEnrichmentSupport.normalizeList(payload.highlights());
            values.put(JobEnrichmentKey.HIGHLIGHTS, objectMapper.valueToTree(highlights));
            JsonNode structuredNode = toJsonNode(payload.structured());
            if (structuredNode != null && !structuredNode.isNull()) {
                values.put(JobEnrichmentKey.STRUCTURED_DATA, structuredNode);
            }
            long end = System.nanoTime();
            Duration latency = Duration.ofNanos(Math.max(0, end - start));
            return JobContentEnrichmentResult.success(values, providerName, latency, fingerprint, List.of());
        } catch (WebClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.warn("DeepSeek enrichment failed for job {} with HTTP {}: {}{}",
                    job.id(), ex.getStatusCode(), ex.getMessage(),
                    StringUtils.hasText(responseBody) ? "; body=" + responseBody : "");
            return JobContentEnrichmentResult.failure(providerName, fingerprint,
                    "HTTP_" + ex.getStatusCode().value(), ex.getMessage());
        } catch (JsonProcessingException ex) {
            log.warn("DeepSeek enrichment returned invalid JSON for job {}: {}", job.id(), ex.getMessage());
            return JobContentEnrichmentResult.failure(providerName, fingerprint, "INVALID_JSON", ex.getMessage());
        } catch (Exception ex) {
            log.warn("DeepSeek enrichment failed for job {}: {}", job.id(), ex.getMessage());
            return JobContentEnrichmentResult.failure(providerName, fingerprint, "UNKNOWN_ERROR", ex.getMessage());
        }
    }

    private DeepSeekRequest buildRequest(JobSnapshot job, String rawContent, String contentText) {
        String userPrompt = JobContentEnrichmentSupport.buildUserPrompt(job, rawContent, contentText);
        List<Message> messages = List.of(
                new Message("system", JobContentEnrichmentSupport.systemPrompt()),
                new Message("user", userPrompt)
        );
        return new DeepSeekRequest(model, messages, temperature, maxTokens);
    }

    private JsonNode toJsonNode(Map<String, Object> structured) {
        if (structured == null || structured.isEmpty()) {
            return null;
        }
        return objectMapper.convertValue(structured, ObjectNode.class);
    }

    private String extractJsonFromResponse(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }

        content = content.trim();

        // Check if content is already valid JSON
        if (content.startsWith("{") && content.endsWith("}")) {
            return content;
        }

        // Try to extract JSON from markdown code blocks
        String[] lines = content.split("\n");
        StringBuilder jsonBuilder = new StringBuilder();
        boolean inJsonBlock = false;
        boolean foundJson = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // Start of JSON block
            if (!inJsonBlock && (trimmed.equals("```json") || trimmed.equals("```"))) {
                inJsonBlock = true;
                continue;
            }

            // End of JSON block
            if (inJsonBlock && trimmed.equals("```")) {
                foundJson = true;
                break;
            }

            // JSON content
            if (inJsonBlock) {
                jsonBuilder.append(line).append("\n");
            }

            // Direct JSON start without markdown
            if (!inJsonBlock && trimmed.startsWith("{")) {
                jsonBuilder.append(line).append("\n");
                inJsonBlock = true;
                foundJson = true;
            }
        }

        if (foundJson) {
            String extracted = jsonBuilder.toString().trim();
            if (StringUtils.hasText(extracted)) {
                return extracted;
            }
        }

        // If no JSON block found, try to find JSON in the content
        int startIndex = content.indexOf('{');
        int endIndex = content.lastIndexOf('}');

        if (startIndex >= 0 && endIndex > startIndex) {
            return content.substring(startIndex, endIndex + 1);
        }

        return null;
    }

    private record DeepSeekRequest(String model,
                                   List<Message> messages,
                                   Double temperature,
                                   Integer max_tokens) {
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
