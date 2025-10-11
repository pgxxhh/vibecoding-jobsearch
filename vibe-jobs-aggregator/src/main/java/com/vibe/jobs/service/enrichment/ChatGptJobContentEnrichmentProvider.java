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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
                                               @Value("${jobs.detail-enhancement.chatgpt.beta-header:responses=v1}") String betaHeader) {
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
    public Optional<JobContentEnrichment> enrich(Job job, String rawContent, String contentText) {
        if (!isEnabled() || job == null) {
            return Optional.empty();
        }
        try {
            ResponsesRequest request = buildRequest(job, rawContent, contentText);
            ResponsesResponse response = webClient.post()
                    .uri(path)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ResponsesResponse.class)
                    .timeout(timeout)
                    .block();
            if (response == null) {
                return Optional.empty();
            }
            String content = extractContent(response);
            if (!StringUtils.hasText(content)) {
                return Optional.empty();
            }
            ChatCompletionPayload payload = objectMapper.readValue(content, ChatCompletionPayload.class);
            String structuredJson = serializeStructured(payload.structured());
            List<String> skills = normalizeList(payload.skills());
            List<String> highlights = normalizeList(payload.highlights());
            return Optional.of(new JobContentEnrichment(
                    normalize(payload.summary()),
                    skills,
                    highlights,
                    structuredJson
            ));
        } catch (WebClientResponseException ex) {
            log.warn("ChatGPT enrichment failed for job {} with HTTP {}: {}", job.getId(), ex.getStatusCode(), ex.getMessage());
            return Optional.empty();
        } catch (JsonProcessingException ex) {
            log.warn("ChatGPT enrichment returned invalid JSON for job {}: {}", job.getId(), ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("ChatGPT enrichment failed for job {}: {}", job.getId(), ex.getMessage());
            return Optional.empty();
        }
    }

    private ResponsesRequest buildRequest(Job job, String rawContent, String contentText) {
        String title = job.getTitle();
        String company = job.getCompany();
        String location = job.getLocation();
        StringBuilder userPrompt = new StringBuilder();
        if (StringUtils.hasText(title)) {
            userPrompt.append("职位: ").append(title).append('\n');
        }
        if (StringUtils.hasText(company)) {
            userPrompt.append("公司: ").append(company).append('\n');
        }
        if (StringUtils.hasText(location)) {
            userPrompt.append("地点: ").append(location).append('\n');
        }
        userPrompt.append("\n职位描述（纯文本）:\n");
        userPrompt.append(truncate(contentText, 6000));
        if (StringUtils.hasText(rawContent)) {
            userPrompt.append("\n\n职位描述（原始 HTML，仅供参考）:\n");
            userPrompt.append(truncate(rawContent, 4000));
        }

        List<InputMessage> input = List.of(
                new InputMessage("system", List.of(Content.ofText(requestContentType, SystemInstructions.TEXT))),
                new InputMessage("user", List.of(Content.ofText(requestContentType, userPrompt.toString())))
        );
        return new ResponsesRequest(model, input, temperature, maxTokens,
                new ResponseFormat("json_object"));
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

    private String serializeStructured(Map<String, Object> structured) throws JsonProcessingException {
        if (structured == null || structured.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(structured);
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().replaceAll("\\s+", " "))
                .collect(Collectors.toList());
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, Math.max(maxChars, 0));
    }

    private record ResponsesRequest(String model,
                                    List<InputMessage> input,
                                    double temperature,
                                    int max_output_tokens,
                                    ResponseFormat response_format) {
    }

    private record InputMessage(String role, List<Content> content) {
    }

    private record Content(String type, String text) {
        static Content ofText(String type, String value) {
            return new Content(type, value);
        }
    }

    private record ResponseFormat(String type) {
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

    private static final class SystemInstructions {
        private static final String TEXT = String.join("\n",
                "你是一个帮助提炼职位描述信息的助手，需输出 JSON 对象，字段包括:",
                "- summary: 200 字以内的中文摘要，突出核心职责与要求。",
                "- skills: 字符串数组，列出 3~8 个关键技能，使用简体中文。",
                "- highlights: 字符串数组，列出亮点或福利，如无则返回空数组。",
                "- structured: JSON 对象，可包含 salary, experienceLevel, employmentType 等可选键，值保持原始语言。",
                "请勿额外输出解释或 markdown。"
        );

        private SystemInstructions() {
        }
    }
}
