package com.vibe.jobs.jobposting.application.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vibe.jobs.jobposting.domain.JobDetail;
import com.vibe.jobs.jobposting.domain.JobDetailEnrichment;
import com.vibe.jobs.jobposting.domain.JobDetailEnrichmentStatus;
import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;
import com.vibe.jobs.jobposting.domain.spi.JobDetailRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class JobDetailEnrichmentWriter {

    private static final Logger log = LoggerFactory.getLogger(JobDetailEnrichmentWriter.class);

    private final JobDetailRepositoryPort repository;
    private final ObjectMapper objectMapper;
    private final JobDetailEnrichmentRetryStrategy retryStrategy;

    public JobDetailEnrichmentWriter(JobDetailRepositoryPort repository,
                                     ObjectMapper objectMapper,
                                     JobDetailEnrichmentRetryStrategy retryStrategy) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.retryStrategy = retryStrategy;
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
            JobDetailEnrichment statusEnrichment = detail.upsertEnrichment(JobEnrichmentKey.STATUS);
            Instant now = Instant.now();
            if (result != null && result.success()) {
                statusEnrichment.markSucceeded(retryStrategy.maxAttempts(), now);
                changed |= applyPayload(detail, result, event);
                changed |= updateStatus(statusEnrichment, buildSuccessStatus(result, event, statusEnrichment));
            } else {
                changed |= handleFailure(statusEnrichment, result, event, now);
            }
            if (changed) {
                repository.save(detail);
            }
        });
    }

    private boolean handleFailure(JobDetailEnrichment statusEnrichment,
                                  JobContentEnrichmentResult result,
                                  JobDetailContentUpdatedEvent event,
                                  Instant now) {
        int maxAttempts = retryStrategy.maxAttempts();
        boolean retriesEnabled = retryStrategy.retriesEnabled() && result != null && result.isRetryable();
        int currentRetryCount = statusEnrichment.getRetryCount();
        if (retriesEnabled) {
            int nextRetryCount = currentRetryCount + 1;
            if (nextRetryCount <= maxAttempts) {
                Duration delay = retryStrategy.calculateDelay(nextRetryCount);
                Instant nextRetryAt = now.plus(delay);
                statusEnrichment.markRetryScheduled(nextRetryCount, nextRetryAt, maxAttempts, now);
                JobDetail detail = statusEnrichment.getJobDetail();
                log.info("Scheduled DeepSeek enrichment retry {} for jobDetail {} at {}", nextRetryCount,
                        detail != null ? detail.getId() : null, nextRetryAt);
            } else {
                statusEnrichment.markFailedTerminal(maxAttempts, currentRetryCount, now);
                JobDetail detail = statusEnrichment.getJobDetail();
                log.warn("DeepSeek enrichment reached max retries for jobDetail {}", detail != null ? detail.getId() : null);
            }
        } else {
            statusEnrichment.markFailedTerminal(maxAttempts, currentRetryCount, now);
        }
        return updateStatus(statusEnrichment, buildFailureStatus(result, event, statusEnrichment));
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

    private boolean updateStatus(JobDetailEnrichment statusEnrichment, ObjectNode statusNode) {
        if (statusNode == null) {
            return false;
        }
        try {
            String statusJson = objectMapper.writeValueAsString(statusNode);
            statusEnrichment.updateValue(statusJson, statusNode.path("provider").asText(null),
                    statusNode.path("sourceFingerprint").asText(null), null, null);
            return true;
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize status for jobDetail {}: {}",
                    statusEnrichment.getJobDetail() != null ? statusEnrichment.getJobDetail().getId() : null,
                    ex.getMessage());
            return false;
        }
    }

    private ObjectNode buildSuccessStatus(JobContentEnrichmentResult result,
                                          JobDetailContentUpdatedEvent event,
                                          JobDetailEnrichment statusEnrichment) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("state", JobDetailEnrichmentStatus.SUCCESS);
        node.put("provider", result != null ? result.provider() : null);
        node.put("sourceFingerprint", event.contentFingerprint());
        node.put("contentVersion", event.contentVersion());
        node.put("updatedAt", Instant.now().toString());
        node.put("retryCount", statusEnrichment.getRetryCount());
        if (statusEnrichment.getMaxAttempts() != null) {
            node.put("maxAttempts", statusEnrichment.getMaxAttempts());
        }
        if (statusEnrichment.getLastAttemptAt() != null) {
            node.put("lastAttemptAt", statusEnrichment.getLastAttemptAt().toString());
        }
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

    private ObjectNode buildFailureStatus(JobContentEnrichmentResult result,
                                          JobDetailContentUpdatedEvent event,
                                          JobDetailEnrichment statusEnrichment) {
        ObjectNode node = objectMapper.createObjectNode();
        String state = statusEnrichment.getStatusState();
        node.put("state", state != null ? state : JobDetailEnrichmentStatus.FAILED);
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
        node.put("retryCount", statusEnrichment.getRetryCount());
        if (statusEnrichment.getMaxAttempts() != null) {
            node.put("maxAttempts", statusEnrichment.getMaxAttempts());
        }
        if (statusEnrichment.getNextRetryAt() != null) {
            node.put("nextRetryAt", statusEnrichment.getNextRetryAt().toString());
        }
        if (statusEnrichment.getLastAttemptAt() != null) {
            node.put("lastAttemptAt", statusEnrichment.getLastAttemptAt().toString());
        }
        return node;
    }
}
