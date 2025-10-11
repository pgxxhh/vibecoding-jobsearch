package com.vibe.jobs.service.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.domain.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 手动集成测试 - 不依赖Spring容器的简单测试
 */
class JobContentEnrichmentMockIntegrationTest {

    private ChatGptJobContentEnrichmentProvider provider;
    private Job testJob;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 创建一个禁用的提供者用于测试
        provider = new ChatGptJobContentEnrichmentProvider(
                objectMapper,
                "", // 空API密钥
                "https://api.openai.com",
                "/v1/responses",
                "gpt-4o-mini",
                Duration.ofSeconds(10),
                0.2,
                800,
                "input_text",
                "output_text",
                "responses-2024-05-21"
        );
        
        testJob = new Job();
        testJob.setId(1L);
        testJob.setTitle("测试职位");
    }

    @Test
    void testProviderDisabledWithoutApiKey() {
        assertThat(provider.isEnabled()).isFalse();
        
        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, "职位描述");
        assertThat(result).isEmpty();
    }

    @Test
    void testClientWithDisabledProvider() {
        JobContentEnrichmentClient client = new JobContentEnrichmentClient(
                true, "chatgpt", java.util.List.of(provider)
        );

        Optional<JobContentEnrichment> result = client.enrich(testJob, null, "职位描述");
        assertThat(result).isEmpty();
    }

    @Test
    void testProviderWithValidApiKey() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 创建一个启用的提供者
        ChatGptJobContentEnrichmentProvider enabledProvider = new ChatGptJobContentEnrichmentProvider(
                objectMapper,
                "sk-test-key", // 测试API密钥
                "https://api.openai.com",
                "/v1/responses",
                "gpt-4o-mini",
                Duration.ofSeconds(10),
                0.2,
                800,
                "input_text",
                "output_text",
                "responses-2024-05-21"
        );
        
        assertThat(enabledProvider.isEnabled()).isTrue();
        assertThat(enabledProvider.name()).isEqualTo("chatgpt");
    }
}