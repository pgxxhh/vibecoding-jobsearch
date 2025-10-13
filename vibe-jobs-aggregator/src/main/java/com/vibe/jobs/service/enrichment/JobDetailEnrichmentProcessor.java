package com.vibe.jobs.service.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.domain.JobDetailEnrichment;
import com.vibe.jobs.repo.JobDetailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
public class JobDetailEnrichmentProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobDetailEnrichmentProcessor.class);
    private static final String SUCCESS_STATE = "SUCCESS";

    private final JobContentEnrichmentClient enrichmentClient;
    private final JobDetailEnrichmentWriter writer;
    private final JobDetailRepository repository;
    private final ObjectMapper objectMapper;

    public JobDetailEnrichmentProcessor(JobContentEnrichmentClient enrichmentClient,
                                        JobDetailEnrichmentWriter writer,
                                        JobDetailRepository repository,
                                        ObjectMapper objectMapper) {
        this.enrichmentClient = enrichmentClient;
        this.writer = writer;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Async("jobContentEnrichmentExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobDetailContentUpdated(JobDetailContentUpdatedEvent event) {
        if (event == null) {
            return;
        }
        log.info("Triggering enrichment for job {} asynchronously", event.jobId());
        
        // Check if enrichment should be skipped
        if (shouldSkipEnrichment(event)) {
            return;
        }
        
        JobContentEnrichmentResult result;
        try {
            result = enrichmentClient.enrich(event.job(), event.rawContent(), event.contentText(), event.contentFingerprint());
        } catch (Exception ex) {
            log.warn("Enrichment client threw exception for job {}: {}", event.jobId(), ex.getMessage());
            result = JobContentEnrichmentResult.failure(null, event.contentFingerprint(), "CLIENT_EXCEPTION", ex.getMessage());
        }
        writer.write(event, result);
    }

    private boolean shouldSkipEnrichment(JobDetailContentUpdatedEvent event) {
        Long jobDetailId = event.jobDetailId();
        if (jobDetailId == null) {
            return false;
        }
        Optional<JobDetail> optionalDetail = repository.findByIdWithEnrichments(jobDetailId);
        if (optionalDetail.isEmpty()) {
            return false;
        }
        JobDetail detail = optionalDetail.get();
        if (!detail.isNotDeleted()) {
            return false;
        }
        String fingerprint = event.contentFingerprint();
        if (!StringUtils.hasText(fingerprint)) {
            return false;
        }
        for (JobDetailEnrichment enrichment : detail.getEnrichments()) {
            if (!fingerprint.equals(enrichment.getSourceFingerprint())) {
                continue;
            }
            if (isSuccessState(enrichment.getValueJson())) {
                log.info("Skip enrichment for jobDetail {} due to matching fingerprint {}", detail.getId(), fingerprint);
                return true;
            }
        }
        return false;
    }

    private boolean isSuccessState(String valueJson) {
        if (!StringUtils.hasText(valueJson)) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            JsonNode state = node.path("state");
            return state.isTextual() && SUCCESS_STATE.equalsIgnoreCase(state.asText().trim());
        } catch (JsonProcessingException ex) {
            log.info("Failed to parse enrichment state JSON: {}", ex.getMessage());
            return false;
        }
    }
}
