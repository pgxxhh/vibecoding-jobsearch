package com.vibe.jobs.crawler.application.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.crawler.domain.AutomationSettings;
import com.vibe.jobs.crawler.domain.CrawlBlueprint;
import com.vibe.jobs.crawler.domain.CrawlFlow;
import com.vibe.jobs.crawler.domain.CrawlStep;
import com.vibe.jobs.crawler.domain.CrawlStepType;
import com.vibe.jobs.crawler.domain.PagingStrategy;
import com.vibe.jobs.crawler.domain.ParserField;
import com.vibe.jobs.crawler.domain.ParserProfile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CrawlerBlueprintConfigFactory {

    private static final int DEFAULT_WAIT_STEP_MS = 5_000;
    private static final int DEFAULT_AUTOMATION_WAIT_MS = 3_000;
    private static final CrawlBlueprint.RateLimit DEFAULT_RATE_LIMIT = CrawlBlueprint.RateLimit.of(10, 1);

    private final ObjectMapper objectMapper;

    public CrawlerBlueprintConfigFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildConfigJson(String entryUrl,
                                  ParserProfile profile,
                                  PagingStrategy pagingStrategy,
                                  AutomationSettings automation,
                                  CrawlFlow flow,
                                  Map<String, Object> metadata,
                                  CrawlBlueprint.RateLimit rateLimit) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("flow", buildFlow(flow));
        root.put("paging", buildPaging(pagingStrategy));
        root.put("parser", buildParser(entryUrl, profile));
        if (entryUrl != null && !entryUrl.isBlank()) {
            root.put("entryUrl", entryUrl.trim());
        }
        root.put("rateLimit", buildRateLimit(rateLimit));
        root.put("automation", buildAutomation(automation));
        if (metadata != null && !metadata.isEmpty()) {
            root.put("metadata", metadata);
        }
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize blueprint config", e);
        }
    }

    private List<Map<String, Object>> buildFlow(CrawlFlow flow) {
        CrawlFlow effective = flow == null || flow.isEmpty() ? defaultFlow() : flow;
        List<Map<String, Object>> steps = new ArrayList<>();
        for (CrawlStep step : effective.steps()) {
            Map<String, Object> stepJson = new LinkedHashMap<>();
            stepJson.put("type", step.type().name());
            Map<String, Object> options = step.options() == null || step.options().isEmpty()
                    ? Map.of()
                    : new LinkedHashMap<>(step.options());
            stepJson.put("options", options);
            steps.add(stepJson);
        }
        return steps;
    }

    private Map<String, Object> buildPaging(PagingStrategy strategy) {
        PagingStrategy effective = strategy == null ? PagingStrategy.disabled() : strategy;
        Map<String, Object> paging = new LinkedHashMap<>();
        paging.put("mode", effective.mode().name());
        paging.put("step", effective.step());
        paging.put("start", effective.start());
        paging.put("parameter", effective.parameter());
        paging.put("sizeParameter", effective.sizeParameter());
        return paging;
    }

    private Map<String, Object> buildParser(String entryUrl, ParserProfile profile) {
        Map<String, Object> parser = new LinkedHashMap<>();
        parser.put("listSelector", profile.listSelector());
        String baseUrl = deriveBaseUrl(entryUrl);
        putIfHasText(parser, "baseUrl", baseUrl);

        Map<String, Object> fields = new LinkedHashMap<>();
        for (Map.Entry<String, ParserField> entry : profile.fields().entrySet()) {
            fields.put(entry.getKey(), buildField(entry.getValue()));
        }
        parser.put("fields", fields);

        List<String> tagFields = new ArrayList<>(profile.tagFields());
        if (tagFields.isEmpty() && profile.fields().containsKey("tags")) {
            tagFields.add("tags");
        }
        parser.put("tagFields", tagFields);

        String descriptionField = profile.descriptionField();
        if ((descriptionField == null || descriptionField.isBlank()) && profile.fields().containsKey("description")) {
            descriptionField = "description";
        }
        parser.put("descriptionField", descriptionField == null ? "" : descriptionField);
        parser.put("detailFetch", buildDetailFetch(profile));
        return parser;
    }

    private Map<String, Object> buildField(ParserField field) {
        Map<String, Object> fieldJson = new LinkedHashMap<>();
        fieldJson.put("type", field.type().name());
        putIfHasText(fieldJson, "selector", field.selector());
        putIfHasText(fieldJson, "attribute", field.attribute());
        putIfHasText(fieldJson, "constant", field.constant());
        putIfHasText(fieldJson, "format", field.format());
        if (field.required()) {
            fieldJson.put("required", true);
        }
        if (field.delimiter() != null && !field.delimiter().isBlank() && !",".equals(field.delimiter())) {
            fieldJson.put("delimiter", field.delimiter());
        }
        putIfHasText(fieldJson, "baseUrl", field.baseUrl());
        return fieldJson;
    }

    private Map<String, Object> buildDetailFetch(ParserProfile profile) {
        ParserProfile.DetailFetchConfig detail = profile.getDetailFetchConfig();
        if (detail == null || !detail.isEnabled()) {
            return Map.of("enabled", false);
        }
        Map<String, Object> detailJson = new LinkedHashMap<>();
        detailJson.put("enabled", true);
        putIfHasText(detailJson, "baseUrl", detail.getBaseUrl());
        putIfHasText(detailJson, "urlField", detail.getUrlField());
        if (detail.getContentSelectors() != null && !detail.getContentSelectors().isEmpty()) {
            detailJson.put("contentSelectors", detail.getContentSelectors());
        }
        if (detail.getDelayMs() > 0) {
            detailJson.put("delayMs", detail.getDelayMs());
        }
        return detailJson;
    }

    private Map<String, Object> buildAutomation(AutomationSettings automation) {
        AutomationSettings effective = automation == null ? AutomationSettings.disabled() : automation;
        Map<String, Object> automationJson = new LinkedHashMap<>();
        automationJson.put("enabled", effective.enabled());
        automationJson.put("jsEnabled", effective.javascriptEnabled());
        putIfHasText(automationJson, "waitForSelector", effective.waitForSelector());
        if (effective.waitForMilliseconds() > 0) {
            automationJson.put("waitForMilliseconds", effective.waitForMilliseconds());
        } else if (automation == null) {
            automationJson.put("waitForMilliseconds", DEFAULT_AUTOMATION_WAIT_MS);
        }
        AutomationSettings.SearchSettings search = effective.search();
        if (search != null && search.enabled()) {
            Map<String, Object> searchJson = new LinkedHashMap<>();
            searchJson.put("enabled", true);
            putIfHasText(searchJson, "submitSelector", search.submitSelector());
            putIfHasText(searchJson, "waitForSelector", search.waitForSelector());
            if (search.waitAfterSubmitMs() > 0) {
                searchJson.put("waitAfterSubmitMs", search.waitAfterSubmitMs());
            }
            List<Map<String, Object>> fields = new ArrayList<>();
            for (AutomationSettings.SearchField field : search.fields()) {
                Map<String, Object> fieldJson = new LinkedHashMap<>();
                putIfHasText(fieldJson, "selector", field.selector());
                putIfHasText(fieldJson, "optionKey", field.optionKey());
                putIfHasText(fieldJson, "constantValue", field.constantValue());
                fieldJson.put("strategy", field.strategy().name());
                fieldJson.put("clearBefore", field.clearBefore());
                fieldJson.put("required", field.required());
                fields.add(fieldJson);
            }
            if (!fields.isEmpty()) {
                searchJson.put("fields", fields);
            }
            automationJson.put("search", searchJson);
        }
        return automationJson;
    }

    private Map<String, Object> buildRateLimit(CrawlBlueprint.RateLimit rateLimit) {
        CrawlBlueprint.RateLimit effective = rateLimit == null ? DEFAULT_RATE_LIMIT : rateLimit;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("burst", effective.burst());
        result.put("requestsPerMinute", effective.requestsPerMinute());
        return result;
    }

    private CrawlFlow defaultFlow() {
        List<CrawlStep> steps = new ArrayList<>();
        Map<String, Object> waitOptions = new LinkedHashMap<>();
        waitOptions.put("durationMs", DEFAULT_WAIT_STEP_MS);
        steps.add(new CrawlStep(CrawlStepType.WAIT, waitOptions));
        steps.add(new CrawlStep(CrawlStepType.EXTRACT_LIST, Map.of()));
        return CrawlFlow.of(steps);
    }

    private void putIfHasText(Map<String, Object> target, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            target.put(key, value);
        }
    }

    private String deriveBaseUrl(String entryUrl) {
        if (entryUrl == null || entryUrl.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(entryUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            builder.append(uri.getScheme().toLowerCase(Locale.ROOT))
                    .append("://")
                    .append(uri.getHost());
            if (uri.getPort() > 0) {
                builder.append(":").append(uri.getPort());
            }
            return builder.toString();
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }
}
