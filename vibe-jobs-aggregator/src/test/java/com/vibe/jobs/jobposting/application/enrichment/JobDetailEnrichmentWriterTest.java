package com.vibe.jobs.jobposting.application.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.shared.infrastructure.config.JobDetailEnrichmentRetryProperties;
import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.jobposting.domain.JobDetail;
import com.vibe.jobs.jobposting.domain.JobDetailEnrichment;
import com.vibe.jobs.jobposting.domain.JobDetailEnrichmentStatus;
import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;
import com.vibe.jobs.jobposting.infrastructure.persistence.JobDetailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobDetailEnrichmentWriterTest {

    private JobDetailRepository repository;
    private ObjectMapper objectMapper;
    private JobDetailEnrichmentRetryStrategy retryStrategy;

    @BeforeEach
    void setUp() {
        repository = mock(JobDetailRepository.class);
        objectMapper = new ObjectMapper();
        JobDetailEnrichmentRetryProperties properties = new JobDetailEnrichmentRetryProperties();
        properties.setInitialDelay(Duration.ofMinutes(1));
        properties.setBackoffMultiplier(2.0);
        properties.setMaxDelay(Duration.ofMinutes(10));
        properties.setMaxAttempts(3);
        retryStrategy = new JobDetailEnrichmentRetryStrategy(properties);
    }

    @Test
    void writeSuccessResetsRetryState() {
        Job job = Job.builder().id(101L).title("Engineer").company("Vibe").build();
        JobDetail detail = new JobDetail(job, "<p>content</p>", "content");
        ReflectionTestUtils.setField(detail, "id", 11L);
        detail.incrementContentVersion();
        JobDetailEnrichment status = detail.upsertEnrichment(JobEnrichmentKey.STATUS);
        status.markRetryScheduled(1, Instant.now().plusSeconds(60), retryStrategy.maxAttempts(), Instant.now());

        when(repository.findById(11L)).thenReturn(Optional.of(detail));
        when(repository.save(detail)).thenReturn(detail);

        JobDetailEnrichmentWriter writer = new JobDetailEnrichmentWriter(repository, objectMapper, retryStrategy);
        JobContentEnrichmentResult result = JobContentEnrichmentResult.success(Map.of(), "deepseek", Duration.ofMillis(10),
                "fp", List.of());
        JobDetailContentUpdatedEvent event = new JobDetailContentUpdatedEvent(11L, job.getId(), JobSnapshot.from(job),
                detail.getContent(), detail.getContentText(), detail.getContentVersion(), "fp");

        writer.write(event, result);

        ArgumentCaptor<JobDetail> captor = ArgumentCaptor.forClass(JobDetail.class);
        verify(repository).save(captor.capture());
        JobDetail saved = captor.getValue();
        JobDetailEnrichment savedStatus = saved.findEnrichment(JobEnrichmentKey.STATUS).orElseThrow();
        assertThat(savedStatus.getStatusState()).isEqualTo(JobDetailEnrichmentStatus.SUCCESS);
        assertThat(savedStatus.getRetryCount()).isZero();
        assertThat(savedStatus.getNextRetryAt()).isNull();
    }

    @Test
    void writeFailureSchedulesRetry() {
        Job job = Job.builder().id(202L).title("Engineer").company("Vibe").build();
        JobDetail detail = new JobDetail(job, "<p>content</p>", "content");
        ReflectionTestUtils.setField(detail, "id", 22L);
        detail.incrementContentVersion();
        JobDetailEnrichment status = detail.upsertEnrichment(JobEnrichmentKey.STATUS);

        when(repository.findById(22L)).thenReturn(Optional.of(detail));
        when(repository.save(detail)).thenReturn(detail);

        JobDetailEnrichmentWriter writer = new JobDetailEnrichmentWriter(repository, objectMapper, retryStrategy);
        JobContentEnrichmentResult result = JobContentEnrichmentResult.failure("deepseek", "fp", "HTTP_429", "Too many requests");
        JobDetailContentUpdatedEvent event = new JobDetailContentUpdatedEvent(22L, job.getId(), JobSnapshot.from(job),
                detail.getContent(), detail.getContentText(), detail.getContentVersion(), "fp");

        writer.write(event, result);

        ArgumentCaptor<JobDetail> captor = ArgumentCaptor.forClass(JobDetail.class);
        verify(repository).save(captor.capture());
        JobDetail saved = captor.getValue();
        JobDetailEnrichment savedStatus = saved.findEnrichment(JobEnrichmentKey.STATUS).orElseThrow();
        assertThat(savedStatus.getStatusState()).isEqualTo(JobDetailEnrichmentStatus.RETRY_SCHEDULED);
        assertThat(savedStatus.getRetryCount()).isEqualTo(1);
        assertThat(savedStatus.getNextRetryAt()).isNotNull();
        assertThat(savedStatus.getNextRetryAt()).isAfter(Instant.now());
    }
}
