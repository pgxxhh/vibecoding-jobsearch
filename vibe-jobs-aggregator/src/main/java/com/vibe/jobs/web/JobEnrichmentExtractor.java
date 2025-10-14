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
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JobEnrichmentExtractor {

    private static final Logger log = LoggerFactory.getLogger(JobEnrichmentExtractor.class);
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private JobEnrichmentExtractor() {
    }

    static EnrichmentView extract(JobDetail detail) {
        if (detail == null) {
            return EnrichmentView.empty();
        }

        Map<JobEnrichmentKey, JsonNode> nodes = new EnumMap<>(JobEnrichmentKey.class);
        Map<String, Object> values = new LinkedHashMap<>();

        if (detail.getEnrichments() != null) {
            for (JobDetailEnrichment enrichment : detail.getEnrichments()) {
                if (enrichment == null || enrichment.getEnrichmentKey() == null) {
                    continue;
                }
                String json = enrichment.getValueJson();
                if (!StringUtils.hasText(json)) {
                    continue;
                }
                Optional<JsonNode> parsed = parseJson(json);
                if (parsed.isEmpty()) {
                    continue;
                }
                JsonNode node = parsed.get();
                nodes.put(enrichment.getEnrichmentKey(), node);
                Object value = treeToObject(node);
                values.put(enrichment.getEnrichmentKey().storageKey(), value);
            }
        }

        Optional<Map<String, Object>> status = Optional.ofNullable(nodes.get(JobEnrichmentKey.STATUS))
                .map(JobEnrichmentExtractor::treeToObject)
                .filter(Map.class::isInstance)
                .map(map -> (Map<String, Object>) map)
                .map(JobEnrichmentExtractor::toUnmodifiableMap);

        boolean ready = status
                .map(JobEnrichmentExtractor::isSuccessState)
                .orElse(true);

        Optional<String> summary = ready
                ? readSummary(nodes.get(JobEnrichmentKey.SUMMARY))
                : Optional.empty();
        List<String> skills = ready
                ? readArray(nodes.get(JobEnrichmentKey.SKILLS))
                : List.of();
        List<String> highlights = ready
                ? readArray(nodes.get(JobEnrichmentKey.HIGHLIGHTS))
                : List.of();
        Optional<String> structured = ready
                ? readStructured(nodes.get(JobEnrichmentKey.STRUCTURED_DATA))
                : Optional.empty();

        Map<String, Object> enrichments = values.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));

        if (summary.isEmpty() && skills.isEmpty() && highlights.isEmpty() && structured.isEmpty()
                && status.isEmpty() && enrichments.isEmpty()) {
            return EnrichmentView.empty();
        }

        return new EnrichmentView(summary,
                skills,
                highlights,
                structured,
                status,
                enrichments);
    }

    static Optional<String> summary(JobDetail detail) {
        return extract(detail).summary();
    }

    static List<String> skills(JobDetail detail) {
        EnrichmentView view = extract(detail);
        return view.skills().isEmpty() ? List.of() : new ArrayList<>(view.skills());
    }

    static List<String> highlights(JobDetail detail) {
        EnrichmentView view = extract(detail);
        return view.highlights().isEmpty() ? List.of() : new ArrayList<>(view.highlights());
    }

    static Optional<String> structured(JobDetail detail) {
        return extract(detail).structured();
    }

    static Map<String, Object> enrichments(JobDetail detail) {
        Map<String, Object> source = extract(detail).enrichments();
        return source.isEmpty() ? Map.of() : new LinkedHashMap<>(source);
    }

    static Optional<Map<String, Object>> status(JobDetail detail) {
        return extract(detail).status()
                .map(map -> map.isEmpty() ? Map.<String, Object>of() : new LinkedHashMap<>(map));
    }

    private static boolean isSuccessState(Map<String, Object> status) {
        Object state = status.get("state");
        if (state instanceof String stateText && StringUtils.hasText(stateText)) {
            return "SUCCESS".equalsIgnoreCase(stateText.trim());
        }
        return true;
    }

    private static List<String> readArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(element -> {
            String text = nodeToText(element);
            if (StringUtils.hasText(text)) {
                values.add(text.trim());
            }
        });
        return values.isEmpty() ? List.of() : List.copyOf(values);
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

    private static Optional<String> readSummary(JsonNode node) {
        return Optional.ofNullable(nodeToText(node))
                .filter(StringUtils::hasText)
                .map(String::trim);
    }

    private static Optional<String> readStructured(JsonNode node) {
        if (node == null) {
            return Optional.empty();
        }
        String value = node.isTextual() ? node.asText() : node.toString();
        return StringUtils.hasText(value) ? Optional.of(value) : Optional.empty();
    }

    private static Map<String, Object> toUnmodifiableMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    static final class EnrichmentView {
        private static final EnrichmentView EMPTY = new EnrichmentView(
                Optional.empty(),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Map.of()
        );

        private final Optional<String> summary;
        private final List<String> skills;
        private final List<String> highlights;
        private final Optional<String> structured;
        private final Optional<Map<String, Object>> status;
        private final Map<String, Object> enrichments;

        private EnrichmentView(Optional<String> summary,
                               List<String> skills,
                               List<String> highlights,
                               Optional<String> structured,
                               Optional<Map<String, Object>> status,
                               Map<String, Object> enrichments) {
            this.summary = summary;
            this.skills = skills;
            this.highlights = highlights;
            this.structured = structured;
            this.status = status;
            this.enrichments = enrichments;
        }

        static EnrichmentView empty() {
            return EMPTY;
        }

        Optional<String> summary() {
            return summary;
        }

        List<String> skills() {
            return skills;
        }

        List<String> highlights() {
            return highlights;
        }

        Optional<String> structured() {
            return structured;
        }

        Optional<Map<String, Object>> status() {
            return status;
        }

        Map<String, Object> enrichments() {
            return enrichments;
        }
    }
}
