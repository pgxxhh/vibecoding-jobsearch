package com.vibe.jobs.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.domain.JobDetailEnrichment;
import com.vibe.jobs.domain.JobEnrichmentKey;
import com.vibe.jobs.service.dto.JobDetailEnrichmentsDto;
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
        return summary(sourceOf(detail));
    }

    static Optional<String> summary(JobDetailEnrichmentsDto detail) {
        return summary(sourceOf(detail));
    }

    static List<String> skills(JobDetail detail) {
        return skills(sourceOf(detail));
    }

    static List<String> skills(JobDetailEnrichmentsDto detail) {
        return skills(sourceOf(detail));
    }

    static List<String> highlights(JobDetail detail) {
        return highlights(sourceOf(detail));
    }

    static List<String> highlights(JobDetailEnrichmentsDto detail) {
        return highlights(sourceOf(detail));
    }

    static Optional<String> structured(JobDetail detail) {
        return structured(sourceOf(detail));
    }

    static Optional<String> structured(JobDetailEnrichmentsDto detail) {
        return structured(sourceOf(detail));
    }

    static Map<String, Object> enrichments(JobDetail detail) {
        return enrichments(sourceOf(detail));
    }

    static Map<String, Object> enrichments(JobDetailEnrichmentsDto detail) {
        return enrichments(sourceOf(detail));
    }

    static Optional<Map<String, Object>> status(JobDetail detail) {
        return status(sourceOf(detail));
    }

    static Optional<Map<String, Object>> status(JobDetailEnrichmentsDto detail) {
        return status(sourceOf(detail));
    }

    private static Optional<String> summary(EnrichmentSource source) {
        if (!isEnrichmentReady(source)) {
            return Optional.empty();
        }
        return readNode(source, JobEnrichmentKey.SUMMARY)
                .map(JobEnrichmentExtractor::nodeToText)
                .filter(StringUtils::hasText)
                .map(String::trim);
    }

    private static List<String> skills(EnrichmentSource source) {
        if (!isEnrichmentReady(source)) {
            return List.of();
        }
        return readArray(source, JobEnrichmentKey.SKILLS);
    }

    private static List<String> highlights(EnrichmentSource source) {
        if (!isEnrichmentReady(source)) {
            return List.of();
        }
        return readArray(source, JobEnrichmentKey.HIGHLIGHTS);
    }

    private static Optional<String> structured(EnrichmentSource source) {
        if (!isEnrichmentReady(source)) {
            return Optional.empty();
        }
        return readNode(source, JobEnrichmentKey.STRUCTURED_DATA)
                .map(node -> node.isTextual() ? node.asText() : node.toString())
                .filter(StringUtils::hasText);
    }

    private static Map<String, Object> enrichments(EnrichmentSource source) {
        if (source == null) {
            return Map.of();
        }
        Map<JobEnrichmentKey, String> values = source.allValues();
        if (values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        values.forEach((key, json) -> {
            if (!StringUtils.hasText(json)) {
                return;
            }
            parseJson(json).ifPresent(node -> {
                Object value = treeToObject(node);
                if (value != null) {
                    map.put(key.storageKey(), value);
                }
            });
        });
        return map;
    }

    private static Optional<Map<String, Object>> status(EnrichmentSource source) {
        return readNode(source, JobEnrichmentKey.STATUS)
                .map(JobEnrichmentExtractor::treeToObject)
                .filter(Map.class::isInstance)
                .map(value -> (Map<String, Object>) value);
    }

    private static boolean isEnrichmentReady(EnrichmentSource source) {
        return status(source)
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

    private static List<String> readArray(EnrichmentSource source, JobEnrichmentKey key) {
        return readNode(source, key)
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

    private static Optional<JsonNode> readNode(EnrichmentSource source, JobEnrichmentKey key) {
        if (source == null || key == null) {
            return Optional.empty();
        }
        return source.findValue(key)
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

    private static EnrichmentSource sourceOf(JobDetail detail) {
        if (detail == null) {
            return null;
        }
        return new EnrichmentSource() {
            @Override
            public Optional<String> findValue(JobEnrichmentKey key) {
                return detail.findEnrichment(key)
                        .map(JobDetailEnrichment::getValueJson);
            }

            @Override
            public Map<JobEnrichmentKey, String> allValues() {
                if (detail.getEnrichments() == null || detail.getEnrichments().isEmpty()) {
                    return Map.of();
                }
                Map<JobEnrichmentKey, String> values = new java.util.EnumMap<>(JobEnrichmentKey.class);
                for (JobDetailEnrichment enrichment : detail.getEnrichments()) {
                    values.put(enrichment.getEnrichmentKey(), enrichment.getValueJson());
                }
                return values;
            }
        };
    }

    private static EnrichmentSource sourceOf(JobDetailEnrichmentsDto detail) {
        if (detail == null) {
            return null;
        }
        return new EnrichmentSource() {
            @Override
            public Optional<String> findValue(JobEnrichmentKey key) {
                return detail.findValue(key);
            }

            @Override
            public Map<JobEnrichmentKey, String> allValues() {
                return detail.enrichmentJsonByKey();
            }
        };
    }

    private interface EnrichmentSource {
        Optional<String> findValue(JobEnrichmentKey key);

        Map<JobEnrichmentKey, String> allValues();
    }
}
