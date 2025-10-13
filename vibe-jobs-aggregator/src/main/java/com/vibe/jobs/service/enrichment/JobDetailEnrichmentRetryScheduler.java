package com.vibe.jobs.service.enrichment;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.domain.JobDetailEnrichment;
import com.vibe.jobs.domain.JobDetailEnrichmentStatus;
import com.vibe.jobs.domain.JobEnrichmentKey;
import com.vibe.jobs.repo.JobDetailEnrichmentRepository;
import com.vibe.jobs.service.JobContentFingerprintCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class JobDetailEnrichmentRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobDetailEnrichmentRetryScheduler.class);

    private final JobDetailEnrichmentRetryStrategy retryStrategy;
    private final JobDetailEnrichmentRepository enrichmentRepository;
    private final JobContentFingerprintCalculator fingerprintCalculator;
    private final ApplicationEventPublisher eventPublisher;

    public JobDetailEnrichmentRetryScheduler(JobDetailEnrichmentRetryStrategy retryStrategy,
                                             JobDetailEnrichmentRepository enrichmentRepository,
                                             JobContentFingerprintCalculator fingerprintCalculator,
                                             ApplicationEventPublisher eventPublisher) {
        this.retryStrategy = retryStrategy;
        this.enrichmentRepository = enrichmentRepository;
        this.fingerprintCalculator = fingerprintCalculator;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${jobs.detail-enhancement.retry.scheduler-interval:PT1M}")
    @Transactional
    public void dispatchRetries() {
        if (!retryStrategy.retriesEnabled()) {
            return;
        }
        Instant now = Instant.now();
        Pageable pageable = PageRequest.of(0, retryStrategy.batchSize(), Sort.by(Sort.Direction.ASC, "nextRetryAt"));
        List<JobDetailEnrichment> candidates = enrichmentRepository
                .findByEnrichmentKeyAndStatusStateAndNextRetryAtLessThanEqual(
                        JobEnrichmentKey.STATUS,
                        JobDetailEnrichmentStatus.RETRY_SCHEDULED,
                        now,
                        pageable);
        if (CollectionUtils.isEmpty(candidates)) {
            return;
        }
        Duration guard = retryStrategy.inFlightGuard();
        for (JobDetailEnrichment enrichment : candidates) {
            if (guard != null && !guard.isZero() && !guard.isNegative()) {
                Instant lastAttempt = enrichment.getLastAttemptAt();
                if (lastAttempt != null && lastAttempt.isAfter(now.minus(guard))) {
                    continue;
                }
            }
            int updated = enrichmentRepository.markRetrying(
                    enrichment.getId(),
                    JobDetailEnrichmentStatus.RETRY_SCHEDULED,
                    JobDetailEnrichmentStatus.RETRYING,
                    now);
            if (updated == 0) {
                continue;
            }
            enrichment.markRetrying(now);
            JobDetail detail = enrichment.getJobDetail();
            if (detail == null || detail.getId() == null) {
                continue;
            }
            Job job = detail.getJob();
            if (job == null || job.getId() == null) {
                continue;
            }
            JobSnapshot snapshot = JobSnapshot.from(job);
            String fingerprint = fingerprintCalculator.compute(job.getId(), detail.getContentText());
            JobDetailContentUpdatedEvent event = new JobDetailContentUpdatedEvent(
                    detail.getId(),
                    job.getId(),
                    snapshot,
                    detail.getContent(),
                    detail.getContentText(),
                    detail.getContentVersion(),
                    fingerprint
            );
            eventPublisher.publishEvent(event);
            log.info("Scheduled retry for jobDetail {} (job {}), retryCount={} next event dispatched", detail.getId(), job.getId(), enrichment.getRetryCount());
        }
    }
}
