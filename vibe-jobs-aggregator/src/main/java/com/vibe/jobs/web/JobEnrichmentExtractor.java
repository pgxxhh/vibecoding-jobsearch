package com.vibe.jobs.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.domain.JobDetailEnrichment;
import com.vibe.jobs.domain.JobEnrichmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JobEnrichmentExtractor {

    private static final Logger log = LoggerFactory.getLogger(JobEnrichmentExtractor.class);
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private JobEnrichmentExtractor() {
    }

    static Optional<String> summary(JobDetail detail) {
        if (!isEnrichmentReady(detail)) {
            return Optional.empty();
        }
        return readNode(detail, JobEnrichmentKey.SUMMARY)
                .map(JobEnrichmentExtractor::nodeToText)
                .filter(StringUtils::hasText)
                .map(String::trim);
    }

    static List<String> skills(JobDetail detail) {
        if (!isEnrichmentReady(detail)) {
            return List.of();
        }
        return readArray(detail, JobEnrichmentKey.SKILLS);
    }

    static List<String> highlights(JobDetail detail) {
        if (!isEnrichmentReady(detail)) {
            return List.of();
        }
        return readArray(detail, JobEnrichmentKey.HIGHLIGHTS);
    }

    static Optional<String> structured(JobDetail detail) {
        if (!isEnrichmentReady(detail)) {
            return Optional.empty();
        }
        return readNode(detail, JobEnrichmentKey.STRUCTURED_DATA)
                .map(node -> node.isTextual() ? node.asText() : node.toString())
                .filter(StringUtils::hasText);
    }

    static Map<String, Object> enrichments(JobDetail detail) {
        if (detail == null || detail.getEnrichments() == null || detail.getEnrichments().isEmpty()) {
            return Map.of();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (JobDetailEnrichment enrichment : detail.getEnrichments()) {
            String json = enrichment.getValueJson();
            if (!StringUtils.hasText(json)) {
                continue;
            }
            parseJson(json).ifPresent(node -> {
                Object value = treeToObject(node);
                map.put(enrichment.getEnrichmentKey().storageKey(), value);
            });
        }
        return map;
    }

    static Optional<Map<String, Object>> status(JobDetail detail) {
        return readNode(detail, JobEnrichmentKey.STATUS)
                .map(JobEnrichmentExtractor::treeToObject)
                .filter(Map.class::isInstance)
                .map(value -> (Map<String, Object>) value);
    }

    private static boolean isEnrichmentReady(JobDetail detail) {
        return status(detail)
                .map(JobEnrichmentExtractor::isSuccessState)
                .orElse(true);
    }

    private static boolean isSuccessState(Map<String, Object> status) {
        Object state = status.get("state");
        if (state instanceof String stateText && StringUtils.hasText(stateText)) {
            return "SUCCESS".equalsIgnoreCase(stateText.trim());
        }
        return true;
    }

    private static List<String> readArray(JobDetail detail, JobEnrichmentKey key) {
        return readNode(detail, key)
                .filter(JsonNode::isArray)
                .map(array -> {
                    List<String> values = new ArrayList<>();
                    array.forEach(node -> {
                        String text = nodeToText(node);
                        if (StringUtils.hasText(text)) {
                            values.add(text.trim());
                        }
                    });
                    return values;
                })
                .orElse(List.of());
    }

    private static Optional<JsonNode> readNode(JobDetail detail, JobEnrichmentKey key) {
        if (detail == null || key == null) {
            return Optional.empty();
        }
        return detail.findEnrichment(key)
                .map(JobDetailEnrichment::getValueJson)
                .flatMap(JobEnrichmentExtractor::parseJson);
    }

    private static Optional<JsonNode> parseJson(String json) {
        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }
        try {
            return Optional.of(MAPPER.readTree(json));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse enrichment JSON: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static String nodeToText(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return Boolean.toString(node.asBoolean());
        }
        return node.toString();
    }

    private static Object treeToObject(JsonNode node) {
        if (node == null) {
            return null;
        }
        try {
            return MAPPER.treeToValue(node, Object.class);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to convert enrichment node to object: {}", ex.getMessage());
            return null;
        }
    }
}
