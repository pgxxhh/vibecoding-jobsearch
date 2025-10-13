package com.vibe.jobs.service.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.domain.JobDetailEnrichment;
import com.vibe.jobs.domain.JobEnrichmentKey;
import com.vibe.jobs.repo.JobDetailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;

@Component
public class JobDetailEnrichmentWriter {

    private static final Logger log = LoggerFactory.getLogger(JobDetailEnrichmentWriter.class);

    private final JobDetailRepository repository;
    private final ObjectMapper objectMapper;

    public JobDetailEnrichmentWriter(JobDetailRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(JobDetailContentUpdatedEvent event, JobContentEnrichmentResult result) {
        if (event == null || event.jobDetailId() == null) {
            return;
        }
        repository.findById(event.jobDetailId()).ifPresent(detail -> {
            if (!detail.isNotDeleted()) {
                return;
            }
            if (detail.getContentVersion() != event.contentVersion()) {
                log.info("Skip enrichment write for jobDetail {} due to version mismatch (expected {}, actual {})",
                        detail.getId(), event.contentVersion(), detail.getContentVersion());
                return;
            }
            boolean changed = false;
            if (result != null && result.success()) {
                changed |= applyPayload(detail, result, event);
                changed |= updateStatus(detail, buildSuccessStatus(result, event));
            } else {
                changed |= updateStatus(detail, buildFailureStatus(result, event));
            }
            if (changed) {
                repository.save(detail);
            }
        });
    }

    private boolean applyPayload(JobDetail detail, JobContentEnrichmentResult result, JobDetailContentUpdatedEvent event) {
        boolean changed = false;
        Map<JobEnrichmentKey, com.fasterxml.jackson.databind.JsonNode> payload = result.payload();
        for (Map.Entry<JobEnrichmentKey, com.fasterxml.jackson.databind.JsonNode> entry : payload.entrySet()) {
            JobEnrichmentKey key = entry.getKey();
            com.fasterxml.jackson.databind.JsonNode value = entry.getValue();
            if (key == null || value == null) {
                continue;
            }
            try {
                String valueJson = objectMapper.writeValueAsString(value);
                String metadata = buildMetadata(result);
                JobDetailEnrichment enrichment = detail.upsertEnrichment(key);
                enrichment.updateValue(valueJson, result.provider(), event.contentFingerprint(), null, metadata);
                changed = true;

            } catch (JsonProcessingException ex) {
                log.warn("Failed to serialize enrichment value for jobDetail {} key {}: {}",
                        detail.getId(), key, ex.getMessage());
            }
        }
        return changed;
    }

    private String buildMetadata(JobContentEnrichmentResult result) throws JsonProcessingException {
        boolean hasLatency = result.latency() != null;
        boolean hasWarnings = result.warnings() != null && !result.warnings().isEmpty();
        if (!hasLatency && !hasWarnings) {
            return null;
        }
        ObjectNode node = objectMapper.createObjectNode();
        if (hasLatency) {
            node.put("latencyMs", result.latency().toMillis());
        }
        if (hasWarnings) {
            ArrayNode warnings = node.putArray("warnings");
            for (String warning : result.warnings()) {
                if (StringUtils.hasText(warning)) {
                    warnings.add(warning);
                }
            }
        }
        return objectMapper.writeValueAsString(node);
    }

    private boolean updateStatus(JobDetail detail, ObjectNode statusNode) {
        if (statusNode == null) {
            return false;
        }
        try {
            String statusJson = objectMapper.writeValueAsString(statusNode);
            JobDetailEnrichment enrichment = detail.upsertEnrichment(JobEnrichmentKey.STATUS);
            enrichment.updateValue(statusJson, statusNode.path("provider").asText(null),
                    statusNode.path("sourceFingerprint").asText(null), null, null);
            return true;
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize status for jobDetail {}: {}", detail.getId(), ex.getMessage());
            return false;
        }
    }

    private ObjectNode buildSuccessStatus(JobContentEnrichmentResult result, JobDetailContentUpdatedEvent event) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("state", "SUCCESS");
        node.put("provider", result != null ? result.provider() : null);
        node.put("sourceFingerprint", event.contentFingerprint());
        node.put("contentVersion", event.contentVersion());
        node.put("updatedAt", Instant.now().toString());
        if (result != null && result.latency() != null) {
            node.put("latencyMs", result.latency().toMillis());
        }
        if (result != null && result.warnings() != null && !result.warnings().isEmpty()) {
            ArrayNode warnings = node.putArray("warnings");
            for (String warning : result.warnings()) {
                if (StringUtils.hasText(warning)) {
                    warnings.add(warning);
                }
            }
        }
        return node;
    }

    private ObjectNode buildFailureStatus(JobContentEnrichmentResult result, JobDetailContentUpdatedEvent event) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("state", "FAILED");
        if (result != null) {
            node.put("provider", result.provider());
            if (result.error() != null) {
                ObjectNode error = node.putObject("error");
                error.put("code", result.error().code());
                error.put("message", result.error().message());
            }
        }
        node.put("sourceFingerprint", event.contentFingerprint());
        node.put("contentVersion", event.contentVersion());
        node.put("updatedAt", Instant.now().toString());
        return node;
    }
}
