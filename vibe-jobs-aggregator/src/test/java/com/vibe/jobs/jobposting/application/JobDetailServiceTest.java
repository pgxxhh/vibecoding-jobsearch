package com.vibe.jobs.jobposting.application;

import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;
import com.vibe.jobs.jobposting.domain.spi.JobDetailRepositoryPort;
import com.vibe.jobs.jobposting.application.dto.JobDetailEnrichmentsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobDetailServiceTest {

    @Mock
    private JobDetailRepositoryPort repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private JobContentFingerprintCalculator fingerprintCalculator;

    @InjectMocks
    private JobDetailService service;

    @BeforeEach
    void setUp() {
        // no-op; service constructed via @InjectMocks
    }

    @Test
    void findByJobIdsAggregatesProjection() {
        when(repository.findEnrichmentsByJobIds(Set.of(1L)))
                .thenReturn(List.of(
                        view(1L, JobEnrichmentKey.SUMMARY, "\"summary\""),
                        view(1L, JobEnrichmentKey.SKILLS, "[\"java\", \"spring\"]")));

        Map<Long, JobDetailEnrichmentsDto> result = service.findByJobIds(List.of(1L, 1L, null));

        assertThat(result).containsOnlyKeys(1L);
        JobDetailEnrichmentsDto dto = result.get(1L);
        assertThat(dto.jobId()).isEqualTo(1L);
        assertThat(dto.findValue(JobEnrichmentKey.SUMMARY)).contains("\"summary\"");
        assertThat(dto.enrichmentJsonByKey())
                .containsEntry(JobEnrichmentKey.SKILLS, "[\"java\", \"spring\"]");

        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(repository).findEnrichmentsByJobIds(captor.capture());
        assertThat(captor.getValue()).containsExactly(1L);
    }

    @Test
    void findByJobIdsReturnsEmptyWhenNoIdsProvided() {
        assertThat(service.findByJobIds(null)).isEmpty();
        assertThat(service.findByJobIds(List.of())).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void findByJobIdsCreatesEmptyDtoWhenNoEnrichments() {
        when(repository.findEnrichmentsByJobIds(Set.of(2L)))
                .thenReturn(List.of(view(2L, null, null)));

        Map<Long, JobDetailEnrichmentsDto> result = service.findByJobIds(Set.of(2L));

        assertThat(result).containsOnlyKeys(2L);
        JobDetailEnrichmentsDto dto = result.get(2L);
        assertThat(dto.enrichmentJsonByKey()).isEmpty();
        assertThat(dto.findValue(JobEnrichmentKey.SUMMARY)).isEmpty();
    }

    private JobDetailRepositoryPort.JobDetailEnrichmentView view(Long jobId, JobEnrichmentKey key, String valueJson) {
        return new JobDetailRepositoryPort.JobDetailEnrichmentView(jobId, key, valueJson);
    }
}
