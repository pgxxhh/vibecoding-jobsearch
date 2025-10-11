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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ChatGptJobContentEnrichmentProvider implements JobContentEnrichmentProvider {

    private static final Logger log = LoggerFactory.getLogger(ChatGptJobContentEnrichmentProvider.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String providerName;
    private final Duration timeout;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final String path;
    private final boolean enabled;
    private final String requestContentType;
    private final String responseTextType;
    private final String responseTextTypeNormalized;
    private final String betaHeader;
    private final ResponseFormat responseFormat;

    public ChatGptJobContentEnrichmentProvider(ObjectMapper objectMapper,
                                               @Value("${jobs.detail-enhancement.chatgpt.api-key:}") String apiKey,
                                               @Value("${jobs.detail-enhancement.chatgpt.base-url:https://api.openai.com}") String baseUrl,
                                               @Value("${jobs.detail-enhancement.chatgpt.path:/v1/responses}") String path,
                                               @Value("${jobs.detail-enhancement.chatgpt.model:gpt-4o-mini}") String model,
                                               @Value("${jobs.detail-enhancement.chatgpt.timeout:PT20S}") Duration timeout,
                                               @Value("${jobs.detail-enhancement.chatgpt.temperature:0.2}") double temperature,
                                               @Value("${jobs.detail-enhancement.chatgpt.max-output-tokens:800}") int maxTokens,
                                               @Value("${jobs.detail-enhancement.chatgpt.request-content-type:input_text}") String requestContentType,
                                               @Value("${jobs.detail-enhancement.chatgpt.response-text-type:output_text}") String responseTextType,
                                               @Value("${jobs.detail-enhancement.chatgpt.beta-header:responses-2024-05-21}") String betaHeader) {
        this.objectMapper = objectMapper;
        this.timeout = timeout;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.path = path;
        this.providerName = "chatgpt";
        this.enabled = StringUtils.hasText(apiKey);
        this.requestContentType = StringUtils.hasText(requestContentType) ? requestContentType.trim() : "input_text";
        this.responseTextType = StringUtils.hasText(responseTextType) ? responseTextType.trim() : "output_text";
        this.responseTextTypeNormalized = this.responseTextType.toLowerCase();
        this.betaHeader = StringUtils.hasText(betaHeader) ? betaHeader.trim() : null;
        this.responseFormat = buildResponseFormat();
        if (this.enabled) {
           this.webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeaders(headers -> {
                        if (StringUtils.hasText(this.betaHeader)) {
                            headers.add("OpenAI-Beta", this.betaHeader);
                        }
                    })
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
            ResponsesRequest request = buildRequest(job, rawContent, contentText);
            if (log.isDebugEnabled()) {
                try {
                    String payload = objectMapper.writeValueAsString(request);
                    log.debug("ChatGPT request payload for job {}: {}", job.getId(), payload);
                } catch (JsonProcessingException ignored) {
                    // ignore payload logging failures
                }
            }
            ResponsesResponse response = webClient.post()
                    .uri(path)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ResponsesResponse.class)
                    .timeout(timeout)
                    .block();
            if (response == null) {
                return JobContentEnrichmentResult.failure(providerName, fingerprint, "EMPTY_RESPONSE",
                        "Provider returned empty response");
            }
            String content = extractContent(response);
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
            return JobContentEnrichmentResult.success(values, providerName,
                    Duration.ofNanos(Math.max(0, end - start)), fingerprint, List.of());
        } catch (WebClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.warn("ChatGPT enrichment failed for job {} with HTTP {}: {}{}",
                    job.getId(), ex.getStatusCode(), ex.getMessage(),
                    StringUtils.hasText(responseBody) ? "; body=" + responseBody : "");
            return JobContentEnrichmentResult.failure(providerName, fingerprint,
                    "HTTP_" + ex.getStatusCode().value(), ex.getMessage());
        } catch (JsonProcessingException ex) {
            log.warn("ChatGPT enrichment returned invalid JSON for job {}: {}", job.getId(), ex.getMessage());
            return JobContentEnrichmentResult.failure(providerName, fingerprint, "INVALID_JSON", ex.getMessage());
        } catch (Exception ex) {
            log.warn("ChatGPT enrichment failed for job {}: {}", job.getId(), ex.getMessage());
            return JobContentEnrichmentResult.failure(providerName, fingerprint, "UNKNOWN_ERROR", ex.getMessage());
        }
    }

    private ResponsesRequest buildRequest(JobSnapshot job, String rawContent, String contentText) {
        String userPrompt = JobContentEnrichmentSupport.buildUserPrompt(job, rawContent, contentText);
        List<InputMessage> input = List.of(
                new InputMessage("system", List.of(Content.ofText(requestContentType, JobContentEnrichmentSupport.systemPrompt()))),
                new InputMessage("user", List.of(Content.ofText(requestContentType, userPrompt)))
        );
        TextOptions textOptions = new TextOptions(responseFormat);
        return new ResponsesRequest(model, input, textOptions, temperature, maxTokens);
    }

    private JsonNode toJsonNode(Map<String, Object> structured) {
        if (structured == null || structured.isEmpty()) {
            return null;
        }
        return objectMapper.convertValue(structured, ObjectNode.class);
    }

    private String extractContent(ResponsesResponse response) {
        if (!CollectionUtils.isEmpty(response.outputText())) {
            return response.outputText().get(0);
        }
        if (CollectionUtils.isEmpty(response.output())) {
            return null;
        }
        String messageContent = response.output().stream()
                .filter(item -> "message".equals(item.type()))
                .findFirst()
                .map(OutputItem::content)
                .filter(content -> !CollectionUtils.isEmpty(content))
                .map(content -> content.stream()
                        .filter(part -> isTextType(part.type()))
                        .map(Content::text)
                        .filter(StringUtils::hasText)
                        .findFirst()
                        .orElse(null))
                .orElse(null);
        if (StringUtils.hasText(messageContent)) {
            return messageContent;
        }
        return response.output().stream()
                .map(OutputItem::content)
                .filter(content -> !CollectionUtils.isEmpty(content))
                .flatMap(List::stream)
                .filter(part -> isTextType(part.type()))
                .map(Content::text)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private boolean isTextType(String type) {
        if (!StringUtils.hasText(type)) {
            return false;
        }
        String normalized = type.trim().toLowerCase();
        if (normalized.equals(responseTextTypeNormalized)) {
            return true;
        }
        return "text".equals(normalized);
    }

    private record ResponsesRequest(String model,
                                    List<InputMessage> input,
                                    TextOptions text,
                                    Double temperature,
                                    Integer max_output_tokens) {
    }

    private record InputMessage(String role, List<Content> content) {
    }

    private record Content(String type, String text) {
        static Content ofText(String type, String value) {
            return new Content(type, value);
        }
    }

    private ResponseFormat buildResponseFormat() {
        return new ResponseFormat("json_schema", JobContentEnrichmentSupport.schemaName(), JobContentEnrichmentSupport.responseSchema());
    }

    private record TextOptions(ResponseFormat format) {
    }

    private record ResponseFormat(String type, String name, Map<String, Object> schema) {
    }

    private record ResponsesResponse(List<OutputItem> output, List<String> output_text) {
        List<String> outputText() {
            return output_text;
        }
    }

    private record OutputItem(String type, String role, List<Content> content) {
    }

    private record ChatCompletionPayload(String summary,
                                         List<String> skills,
                                         List<String> highlights,
                                         Map<String, Object> structured) {
    }

}
