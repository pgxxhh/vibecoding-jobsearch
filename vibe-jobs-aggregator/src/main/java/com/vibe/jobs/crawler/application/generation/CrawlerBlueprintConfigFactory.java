package com.vibe.jobs.crawler.application.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.crawler.domain.AutomationSettings;
import com.vibe.jobs.crawler.domain.CrawlFlow;
import com.vibe.jobs.crawler.domain.CrawlStep;
import com.vibe.jobs.crawler.domain.CrawlStepType;
import com.vibe.jobs.crawler.domain.PagingStrategy;
import com.vibe.jobs.crawler.domain.ParserField;
import com.vibe.jobs.crawler.domain.ParserProfile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CrawlerBlueprintConfigFactory {

    private final ObjectMapper objectMapper;

    public CrawlerBlueprintConfigFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildConfigJson(String entryUrl,
                                  ParserProfile profile,
                                  PagingStrategy pagingStrategy,
                                  AutomationSettings automation,
                                  CrawlFlow flow,
                                  Map<String, Object> metadata) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (entryUrl != null && !entryUrl.isBlank()) {
            root.put("entryUrl", entryUrl);
        }
        if (pagingStrategy != null && pagingStrategy.mode() != PagingStrategy.Mode.NONE) {
            Map<String, Object> paging = new LinkedHashMap<>();
            paging.put("mode", pagingStrategy.mode().name());
            paging.put("parameter", pagingStrategy.parameter());
            paging.put("start", pagingStrategy.start());
            paging.put("step", pagingStrategy.step());
            paging.put("sizeParameter", pagingStrategy.sizeParameter());
            root.put("paging", paging);
        }
        root.put("parser", buildParser(profile));
        if (metadata != null && !metadata.isEmpty()) {
            root.put("metadata", metadata);
        }
        if (flow != null && !flow.isEmpty()) {
            List<Map<String, Object>> steps = new ArrayList<>();
            for (CrawlStep step : flow.steps()) {
                Map<String, Object> stepJson = new LinkedHashMap<>();
                stepJson.put("type", step.type().name());
                stepJson.put("options", step.options());
                steps.add(stepJson);
            }
            root.put("flow", steps);
        }
        if (automation != null && automation.enabled()) {
            root.put("automation", buildAutomation(automation));
        }
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize blueprint config", e);
        }
    }

    private Map<String, Object> buildParser(ParserProfile profile) {
        Map<String, Object> parser = new LinkedHashMap<>();
        parser.put("listSelector", profile.listSelector());
        Map<String, Object> fields = new LinkedHashMap<>();
        for (Map.Entry<String, ParserField> entry : profile.fields().entrySet()) {
            fields.put(entry.getKey(), buildField(entry.getValue()));
        }
        parser.put("fields", fields);
        if (!profile.getDetailFetchConfig().isEnabled()) {
            parser.put("detailFetch", Map.of("enabled", false));
        } else {
            ParserProfile.DetailFetchConfig detail = profile.getDetailFetchConfig();
            Map<String, Object> detailJson = new LinkedHashMap<>();
            detailJson.put("enabled", true);
            detailJson.put("baseUrl", detail.getBaseUrl());
            detailJson.put("urlField", detail.getUrlField());
            detailJson.put("contentSelectors", detail.getContentSelectors());
            detailJson.put("delayMs", detail.getDelayMs());
            parser.put("detailFetch", detailJson);
        }
        if (profile.fields().containsKey("tags")) {
            parser.put("tagFields", List.of("tags"));
        }
        if (!profile.listSelector().isBlank() && profile.fields().containsKey("description")) {
            parser.put("descriptionField", "description");
        }
        return parser;
    }

    private Map<String, Object> buildField(ParserField field) {
        Map<String, Object> fieldJson = new LinkedHashMap<>();
        fieldJson.put("type", field.type().name());
        fieldJson.put("selector", field.selector());
        fieldJson.put("attribute", field.attribute());
        fieldJson.put("constant", field.constant());
        fieldJson.put("format", field.format());
        fieldJson.put("delimiter", field.delimiter());
        fieldJson.put("required", field.required());
        if (field.baseUrl() != null && !field.baseUrl().isBlank()) {
            fieldJson.put("baseUrl", field.baseUrl());
        }
        return fieldJson;
    }

    private Map<String, Object> buildAutomation(AutomationSettings automation) {
        Map<String, Object> automationJson = new LinkedHashMap<>();
        automationJson.put("enabled", automation.enabled());
        automationJson.put("jsEnabled", automation.javascriptEnabled());
        automationJson.put("waitForSelector", automation.waitForSelector());
        automationJson.put("waitForMilliseconds", automation.waitForMilliseconds());
        AutomationSettings.SearchSettings search = automation.search();
        if (search != null && search.enabled()) {
            Map<String, Object> searchJson = new LinkedHashMap<>();
            searchJson.put("enabled", true);
            searchJson.put("submitSelector", search.submitSelector());
            searchJson.put("waitForSelector", search.waitForSelector());
            searchJson.put("waitAfterSubmitMs", search.waitAfterSubmitMs());
            List<Map<String, Object>> fields = new ArrayList<>();
            for (AutomationSettings.SearchField field : search.fields()) {
                Map<String, Object> fieldJson = new LinkedHashMap<>();
                fieldJson.put("selector", field.selector());
                fieldJson.put("optionKey", field.optionKey());
                fieldJson.put("constantValue", field.constantValue());
                fieldJson.put("strategy", field.strategy().name());
                fieldJson.put("clearBefore", field.clearBefore());
                fieldJson.put("required", field.required());
                fields.add(fieldJson);
            }
            searchJson.put("fields", fields);
            automationJson.put("search", searchJson);
        }
        return automationJson;
    }
}
