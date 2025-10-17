package com.vibe.jobs.jobposting.application.enrichment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobContentEnrichmentClientTest {

    @Mock
    private JobContentEnrichmentProvider mockProvider1;

    @Mock
    private JobContentEnrichmentProvider mockProvider2;

    private JobSnapshot snapshot;
    private JobContentEnrichmentResult enrichmentResult;

    @BeforeEach
    void setUp() {
        snapshot = new JobSnapshot(1L, "测试职位", "测试公司", "北京", "Senior", "https://example.com", List.of("Java"));
        enrichmentResult = JobContentEnrichmentResult.success(
                Map.of(),
                "provider",
                Duration.ofMillis(500),
                "fingerprint",
                List.of()
        );
    }

    @Test
    void testClientDisabled() {
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(false, "chatgpt", List.of(mockProvider1));

        JobContentEnrichmentResult result = client.enrich(snapshot, null, "内容", "fp");

        assertThat(result.success()).isFalse();
        verify(mockProvider1, never()).enrich(any(), anyString(), anyString(), anyString());
    }

    @Test
    void testClientWithNullJob() {
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(true, "chatgpt", List.of(mockProvider1));

        JobContentEnrichmentResult result = client.enrich(null, null, "内容", "fp");

        assertThat(result.success()).isFalse();
        verify(mockProvider1, never()).enrich(any(), anyString(), anyString(), anyString());
    }

    @Test
    void testClientWithNoProviders() {
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(true, "chatgpt", List.of());

        JobContentEnrichmentResult result = client.enrich(snapshot, null, "内容", "fp");

        assertThat(result.success()).isFalse();
    }

    @Test
    void testClientWithSpecificProvider() {
        when(mockProvider1.name()).thenReturn("chatgpt");
        when(mockProvider1.isEnabled()).thenReturn(true);
        when(mockProvider1.enrich(snapshot, null, "内容", "fp")).thenReturn(enrichmentResult);

        when(mockProvider2.name()).thenReturn("claude");

        JobContentEnrichmentClient client = new JobContentEnrichmentClient(true, "chatgpt", List.of(mockProvider1, mockProvider2));

        JobContentEnrichmentResult result = client.enrich(snapshot, null, "内容", "fp");

        assertThat(result).isEqualTo(enrichmentResult);
        verify(mockProvider1).enrich(snapshot, null, "内容", "fp");
        verify(mockProvider2, never()).enrich(any(), anyString(), anyString(), anyString());
    }

    @Test
    void testClientWithSpecificProviderDisabled() {
        when(mockProvider1.name()).thenReturn("chatgpt");
        when(mockProvider1.isEnabled()).thenReturn(false);

        when(mockProvider2.name()).thenReturn("claude");
        when(mockProvider2.isEnabled()).thenReturn(true);
        when(mockProvider2.enrich(snapshot, null, "内容", "fp")).thenReturn(enrichmentResult);

        JobContentEnrichmentClient client = new JobContentEnrichmentClient(true, "chatgpt", List.of(mockProvider1, mockProvider2));

        JobContentEnrichmentResult result = client.enrich(snapshot, null, "内容", "fp");

        assertThat(result).isEqualTo(enrichmentResult);
        verify(mockProvider1, never()).enrich(any(), anyString(), anyString(), anyString());
        verify(mockProvider2).enrich(snapshot, null, "内容", "fp");
    }

    @Test
    void testClientWithUnknownProvider() {
        when(mockProvider1.name()).thenReturn("chatgpt");
        when(mockProvider1.isEnabled()).thenReturn(true);
        when(mockProvider1.enrich(snapshot, null, "内容", "fp")).thenReturn(enrichmentResult);

        JobContentEnrichmentClient client = new JobContentEnrichmentClient(true, "unknown-provider", List.of(mockProvider1));

        JobContentEnrichmentResult result = client.enrich(snapshot, null, "内容", "fp");

        assertThat(result).isEqualTo(enrichmentResult);
        verify(mockProvider1).enrich(snapshot, null, "内容", "fp");
    }

    @Test
    void testClientFallbackToFirstEnabled() {
        when(mockProvider1.name()).thenReturn("provider1");
        when(mockProvider1.isEnabled()).thenReturn(false);

        when(mockProvider2.name()).thenReturn("provider2");
        when(mockProvider2.isEnabled()).thenReturn(true);
        when(mockProvider2.enrich(snapshot, null, "内容", "fp")).thenReturn(enrichmentResult);

        JobContentEnrichmentClient client = new JobContentEnrichmentClient(true, null, List.of(mockProvider1, mockProvider2));

        JobContentEnrichmentResult result = client.enrich(snapshot, null, "内容", "fp");

        assertThat(result).isEqualTo(enrichmentResult);
        verify(mockProvider2).enrich(snapshot, null, "内容", "fp");
    }

    @Test
    void testClientAllProvidersDisabled() {
        when(mockProvider1.name()).thenReturn("provider1");
        when(mockProvider1.isEnabled()).thenReturn(false);

        when(mockProvider2.name()).thenReturn("provider2");
        when(mockProvider2.isEnabled()).thenReturn(false);

        JobContentEnrichmentClient client = new JobContentEnrichmentClient(true, null, List.of(mockProvider1, mockProvider2));

        JobContentEnrichmentResult result = client.enrich(snapshot, null, "内容", "fp");

        assertThat(result.success()).isFalse();
    }

    @Test
    void testProviderNameNormalization() {
        when(mockProvider1.name()).thenReturn("ChatGPT");
        when(mockProvider1.isEnabled()).thenReturn(true);
        when(mockProvider1.enrich(snapshot, null, "内容", "fp")).thenReturn(enrichmentResult);

        JobContentEnrichmentClient client = new JobContentEnrichmentClient(true, "chatgpt", List.of(mockProvider1));

        JobContentEnrichmentResult result = client.enrich(snapshot, null, "内容", "fp");

        assertThat(result).isEqualTo(enrichmentResult);
        verify(mockProvider1).enrich(snapshot, null, "内容", "fp");
    }

    @Test
    void testProviderWithNullOrEmptyName() {
        when(mockProvider1.name()).thenReturn(null);
        when(mockProvider2.name()).thenReturn("");

        JobContentEnrichmentProvider mockProvider3 = org.mockito.Mockito.mock(JobContentEnrichmentProvider.class);
        when(mockProvider3.name()).thenReturn("valid");
        when(mockProvider3.isEnabled()).thenReturn(true);
        when(mockProvider3.enrich(snapshot, null, "内容", "fp")).thenReturn(enrichmentResult);

        JobContentEnrichmentClient client = new JobContentEnrichmentClient(true, "valid", List.of(mockProvider1, mockProvider2, mockProvider3));

        JobContentEnrichmentResult result = client.enrich(snapshot, null, "内容", "fp");

        assertThat(result).isEqualTo(enrichmentResult);
        verify(mockProvider3).enrich(snapshot, null, "内容", "fp");
    }
}
