package com.vibe.jobs.service.enrichment;

import com.vibe.jobs.domain.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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
    
    private Job testJob;
    private JobContentEnrichment testEnrichment;

    @BeforeEach
    void setUp() {
        testJob = new Job();
        testJob.setId(1L);
        testJob.setTitle("测试职位");
        
        testEnrichment = new JobContentEnrichment(
                "测试摘要",
                List.of("Java", "Spring"),
                List.of("高薪"),
                "{\"salary\": \"20k\"}"
        );
    }

    @Test
    void testClientDisabled() {
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(
                false, "chatgpt", List.of(mockProvider1)
        );
        
        Optional<JobContentEnrichment> result = client.enrich(testJob, null, "内容");
        
        assertThat(result).isEmpty();
        verify(mockProvider1, never()).enrich(any(), anyString(), anyString());
    }

    @Test
    void testClientWithNullJob() {
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(
                true, "chatgpt", List.of(mockProvider1)
        );
        
        Optional<JobContentEnrichment> result = client.enrich(null, null, "内容");
        
        assertThat(result).isEmpty();
        verify(mockProvider1, never()).enrich(any(), anyString(), anyString());
    }

    @Test
    void testClientWithNoProviders() {
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(
                true, "chatgpt", List.of()
        );
        
        Optional<JobContentEnrichment> result = client.enrich(testJob, null, "内容");
        
        assertThat(result).isEmpty();
    }

    @Test
    void testClientWithSpecificProvider() {
        when(mockProvider1.name()).thenReturn("chatgpt");
        when(mockProvider1.isEnabled()).thenReturn(true);
        when(mockProvider1.enrich(testJob, null, "内容")).thenReturn(Optional.of(testEnrichment));
        
        when(mockProvider2.name()).thenReturn("claude");
        
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(
                true, "chatgpt", List.of(mockProvider1, mockProvider2)
        );
        
        Optional<JobContentEnrichment> result = client.enrich(testJob, null, "内容");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testEnrichment);
        verify(mockProvider1).enrich(testJob, null, "内容");
        verify(mockProvider2, never()).enrich(any(), anyString(), anyString());
    }

    @Test
    void testClientWithSpecificProviderDisabled() {
        when(mockProvider1.name()).thenReturn("chatgpt");
        when(mockProvider1.isEnabled()).thenReturn(false);
        
        when(mockProvider2.name()).thenReturn("claude");
        when(mockProvider2.isEnabled()).thenReturn(true);
        when(mockProvider2.enrich(testJob, null, "内容")).thenReturn(Optional.of(testEnrichment));
        
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(
                true, "chatgpt", List.of(mockProvider1, mockProvider2)
        );
        
        Optional<JobContentEnrichment> result = client.enrich(testJob, null, "内容");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testEnrichment);
        verify(mockProvider1, never()).enrich(any(), anyString(), anyString());
        verify(mockProvider2).enrich(testJob, null, "内容");
    }

    @Test
    void testClientWithUnknownProvider() {
        when(mockProvider1.name()).thenReturn("chatgpt");
        when(mockProvider1.isEnabled()).thenReturn(true);
        when(mockProvider1.enrich(testJob, null, "内容")).thenReturn(Optional.of(testEnrichment));
        
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(
                true, "unknown-provider", List.of(mockProvider1)
        );
        
        Optional<JobContentEnrichment> result = client.enrich(testJob, null, "内容");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testEnrichment);
        verify(mockProvider1).enrich(testJob, null, "内容");
    }

    @Test
    void testClientFallbackToFirstEnabled() {
        when(mockProvider1.name()).thenReturn("provider1");
        when(mockProvider1.isEnabled()).thenReturn(false);
        
        when(mockProvider2.name()).thenReturn("provider2");
        when(mockProvider2.isEnabled()).thenReturn(true);
        when(mockProvider2.enrich(testJob, null, "内容")).thenReturn(Optional.of(testEnrichment));
        
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(
                true, null, List.of(mockProvider1, mockProvider2)
        );
        
        Optional<JobContentEnrichment> result = client.enrich(testJob, null, "内容");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testEnrichment);
        verify(mockProvider2).enrich(testJob, null, "内容");
    }

    @Test
    void testClientAllProvidersDisabled() {
        when(mockProvider1.name()).thenReturn("provider1");
        when(mockProvider1.isEnabled()).thenReturn(false);
        
        when(mockProvider2.name()).thenReturn("provider2");
        when(mockProvider2.isEnabled()).thenReturn(false);
        
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(
                true, null, List.of(mockProvider1, mockProvider2)
        );
        
        Optional<JobContentEnrichment> result = client.enrich(testJob, null, "内容");
        
        assertThat(result).isEmpty();
    }

    @Test
    void testProviderNameNormalization() {
        when(mockProvider1.name()).thenReturn("ChatGPT"); // 大小写混合
        when(mockProvider1.isEnabled()).thenReturn(true);
        when(mockProvider1.enrich(testJob, null, "内容")).thenReturn(Optional.of(testEnrichment));
        
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(
                true, "chatgpt", List.of(mockProvider1) // 小写配置
        );
        
        Optional<JobContentEnrichment> result = client.enrich(testJob, null, "内容");
        
        assertThat(result).isPresent();
        verify(mockProvider1).enrich(testJob, null, "内容");
    }

    @Test
    void testProviderWithNullOrEmptyName() {
        when(mockProvider1.name()).thenReturn(null);
        when(mockProvider2.name()).thenReturn("");
        
        JobContentEnrichmentProvider mockProvider3 = org.mockito.Mockito.mock(JobContentEnrichmentProvider.class);
        when(mockProvider3.name()).thenReturn("valid");
        when(mockProvider3.isEnabled()).thenReturn(true);
        when(mockProvider3.enrich(testJob, null, "内容")).thenReturn(Optional.of(testEnrichment));
        
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(
                true, "valid", List.of(mockProvider1, mockProvider2, mockProvider3)
        );
        
        Optional<JobContentEnrichment> result = client.enrich(testJob, null, "内容");
        
        assertThat(result).isPresent();
        verify(mockProvider3).enrich(testJob, null, "内容");
    }
}