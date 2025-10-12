package com.vibe.jobs.service.enrichment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class JobDetailEnrichmentProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobDetailEnrichmentProcessor.class);

    private final JobContentEnrichmentClient enrichmentClient;
    private final JobDetailEnrichmentWriter writer;

    public JobDetailEnrichmentProcessor(JobContentEnrichmentClient enrichmentClient,
                                        JobDetailEnrichmentWriter writer) {
        this.enrichmentClient = enrichmentClient;
        this.writer = writer;
    }

    @Async("jobContentEnrichmentExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobDetailContentUpdated(JobDetailContentUpdatedEvent event) {
        if (event == null) {
            return;
        }
        log.debug("Triggering enrichment for job {} asynchronously", event.jobId());
        JobContentEnrichmentResult result;
        try {
            result = enrichmentClient.enrich(event.job(), event.rawContent(), event.contentText(), event.contentFingerprint());
        } catch (Exception ex) {
            log.warn("Enrichment client threw exception for job {}: {}", event.jobId(), ex.getMessage());
            result = JobContentEnrichmentResult.failure(null, event.contentFingerprint(), "CLIENT_EXCEPTION", ex.getMessage());
        }
        writer.write(event, result);
    }
}
