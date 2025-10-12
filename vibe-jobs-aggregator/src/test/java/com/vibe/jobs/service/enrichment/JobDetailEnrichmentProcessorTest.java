package com.vibe.jobs.service.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.domain.JobEnrichmentKey;
import com.vibe.jobs.repo.JobDetailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobDetailEnrichmentProcessorTest {

    @Mock
    private JobContentEnrichmentClient enrichmentClient;

    @Mock
    private JobDetailEnrichmentWriter writer;

    @Mock
    private JobDetailRepository repository;

    private ObjectMapper objectMapper;

    private JobDetailEnrichmentProcessor processor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        processor = new JobDetailEnrichmentProcessor(enrichmentClient, writer, repository, objectMapper);
    }

    @Test
    void shouldSkipEnrichmentWhenFingerprintMatchesAndStatusSuccess() throws Exception {
        String fingerprint = "fp-123";
        JobDetail detail = createJobDetailWithStatus(fingerprint, "SUCCESS");
        when(repository.findByIdWithEnrichments(1L)).thenReturn(Optional.of(detail));

        JobDetailContentUpdatedEvent event = new JobDetailContentUpdatedEvent(
                1L,
                1L,
                createSnapshot(),
                "raw",
                "text",
                0L,
                fingerprint
        );

        processor.onJobDetailContentUpdated(event);

        verify(enrichmentClient, never()).enrich(any(), any(), any(), any());
        verify(writer, never()).write(any(), any());
    }

    @Test
    void shouldInvokeEnrichmentWhenFingerprintChanged() {
        String oldFingerprint = "fp-old";
        String newFingerprint = "fp-new";
        JobDetail detail = createJobDetailWithStatus(oldFingerprint, "SUCCESS");
        when(repository.findByIdWithEnrichments(1L)).thenReturn(Optional.of(detail));

        JobDetailContentUpdatedEvent event = new JobDetailContentUpdatedEvent(
                1L,
                1L,
                createSnapshot(),
                "raw",
                "text",
                0L,
                newFingerprint
        );

        JobContentEnrichmentResult enrichmentResult = JobContentEnrichmentResult.success(
                Map.of(),
                "provider",
                Duration.ZERO,
                newFingerprint,
                List.of()
        );
        when(enrichmentClient.enrich(event.job(), event.rawContent(), event.contentText(), event.contentFingerprint()))
                .thenReturn(enrichmentResult);

        processor.onJobDetailContentUpdated(event);

        verify(enrichmentClient).enrich(event.job(), event.rawContent(), event.contentText(), event.contentFingerprint());
        ArgumentCaptor<JobContentEnrichmentResult> resultCaptor = ArgumentCaptor.forClass(JobContentEnrichmentResult.class);
        verify(writer).write(eq(event), resultCaptor.capture());
        assertThat(resultCaptor.getValue()).isEqualTo(enrichmentResult);
    }

    private JobDetail createJobDetailWithStatus(String fingerprint, String state) {
        Job job = TestUtils.createTestJob();
        JobDetail detail = new JobDetail(job, "content", "text");
        detail.upsertEnrichment(JobEnrichmentKey.STATUS)
                .updateValue(stateJson(state), "provider", fingerprint, null, null);
        return detail;
    }

    private String stateJson(String state) {
        try {
            return objectMapper.writeValueAsString(Map.of("state", state));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private JobSnapshot createSnapshot() {
        Job job = TestUtils.createTestJob();
        return new JobSnapshot(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                null,
                null,
                List.of()
        );
    }
}
