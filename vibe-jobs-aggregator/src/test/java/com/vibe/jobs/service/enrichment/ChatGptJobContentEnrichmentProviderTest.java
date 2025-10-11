package com.vibe.jobs.service.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.domain.Job;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChatGptJobContentEnrichmentProviderTest {

    private MockWebServer mockWebServer;
    private ChatGptJobContentEnrichmentProvider provider;
    private ObjectMapper objectMapper;
    private Job testJob;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        objectMapper = new ObjectMapper();
        
        String baseUrl = mockWebServer.url("/").toString();
        provider = new ChatGptJobContentEnrichmentProvider(
                objectMapper,
                "test-api-key",
                baseUrl,
                "/v1/responses",
                "gpt-4o-mini",
                Duration.ofSeconds(10),
                0.2,
                800
        );
        
        testJob = new Job();
        testJob.setId(1L);
        testJob.setTitle("Java 开发工程师");
        testJob.setCompany("技术公司");
        testJob.setLocation("北京");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testProviderName() {
        assertThat(provider.name()).isEqualTo("chatgpt");
    }

    @Test
    void testIsEnabledWithApiKey() {
        assertThat(provider.isEnabled()).isTrue();
    }

    @Test
    void testIsEnabledWithoutApiKey() {
        ChatGptJobContentEnrichmentProvider providerWithoutKey = new ChatGptJobContentEnrichmentProvider(
                objectMapper, "", "https://api.openai.com", "/v1/responses", "gpt-4o-mini",
                Duration.ofSeconds(10), 0.2, 800
        );
        assertThat(providerWithoutKey.isEnabled()).isFalse();
    }

    @Test
    void testSuccessfulEnrichment() throws Exception {
        // 准备模拟响应
        String mockResponseJson = """
                {
                    "output_text": [
                        "{\\"summary\\": \\"负责Java后端开发，参与系统架构设计和优化\\", \\"skills\\": [\\"Java\\", \\"Spring Boot\\", \\"MySQL\\", \\"Redis\\"], \\"highlights\\": [\\"五险一金\\", \\"弹性工作制\\", \\"技术培训\\"], \\"structured\\": {\\"salary\\": \\"15k-25k\\", \\"experienceLevel\\": \\"3-5年\\", \\"employmentType\\": \\"全职\\"}}"
                    ]
                }
                """;
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .setHeader("Content-Type", "application/json"));

        Optional<JobContentEnrichment> result = provider.enrich(testJob, "<html>原始HTML内容</html>", "纯文本职位描述");
        
        assertThat(result).isPresent();
        JobContentEnrichment enrichment = result.get();
        
        assertThat(enrichment.summary()).isEqualTo("负责Java后端开发，参与系统架构设计和优化");
        assertThat(enrichment.skills()).containsExactly("Java", "Spring Boot", "MySQL", "Redis");
        assertThat(enrichment.highlights()).containsExactly("五险一金", "弹性工作制", "技术培训");
        assertThat(enrichment.structuredData()).isNotNull();
        
        // 验证请求
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/v1/responses");
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-api-key");
        assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json");
    }

    @Test
    void testEnrichmentWithOutputFormat() throws Exception {
        // 测试使用output格式的响应
        String mockResponseJson = """
                {
                    "output": [
                        {
                            "type": "message",
                            "role": "assistant",
                            "content": [
                                {
                                    "type": "text",
                                    "text": "{\\"summary\\": \\"高级Java工程师职位\\", \\"skills\\": [\\"Java\\", \\"Spring\\"], \\"highlights\\": [\\"高薪\\"], \\"structured\\": {}}"
                                }
                            ]
                        }
                    ]
                }
                """;
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .setHeader("Content-Type", "application/json"));

        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, "职位描述");
        
        assertThat(result).isPresent();
        assertThat(result.get().summary()).isEqualTo("高级Java工程师职位");
    }

    @Test
    void testEnrichmentWithEmptySkillsAndHighlights() throws Exception {
        String mockResponseJson = """
                {
                    "output_text": [
                        "{\\"summary\\": \\"简单职位描述\\", \\"skills\\": [], \\"highlights\\": [], \\"structured\\": {}}"
                    ]
                }
                """;
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .setHeader("Content-Type", "application/json"));

        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, "职位描述");
        
        assertThat(result).isPresent();
        JobContentEnrichment enrichment = result.get();
        assertThat(enrichment.skills()).isEmpty();
        assertThat(enrichment.highlights()).isEmpty();
    }

    @Test
    void testEnrichmentWithHttpError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, "职位描述");
        
        assertThat(result).isEmpty();
    }

    @Test
    void testEnrichmentWithInvalidJson() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("invalid json")
                .setHeader("Content-Type", "application/json"));

        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, "职位描述");
        
        assertThat(result).isEmpty();
    }

    @Test
    void testEnrichmentWithNullJob() {
        Optional<JobContentEnrichment> result = provider.enrich(null, null, "职位描述");
        assertThat(result).isEmpty();
    }

    @Test
    void testEnrichmentWithEmptyResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{}")
                .setHeader("Content-Type", "application/json"));

        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, "职位描述");
        assertThat(result).isEmpty();
    }

    @Test
    void testRequestPayloadGeneration() throws Exception {
        String mockResponseJson = """
                {
                    "output_text": [
                        "{\\"summary\\": \\"test\\", \\"skills\\": [], \\"highlights\\": [], \\"structured\\": {}}"
                    ]
                }
                """;
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .setHeader("Content-Type", "application/json"));

        provider.enrich(testJob, "<p>原始HTML</p>", "纯文本内容");
        
        RecordedRequest request = mockWebServer.takeRequest();
        String requestBody = request.getBody().readUtf8();
        
        // 验证请求包含必要字段
        assertThat(requestBody).contains("gpt-4o-mini");
        assertThat(requestBody).contains("Java 开发工程师");
        assertThat(requestBody).contains("技术公司");
        assertThat(requestBody).contains("北京");
        assertThat(requestBody).contains("纯文本内容");
        assertThat(requestBody).contains("原始HTML");
        assertThat(requestBody).contains("temperature");
        assertThat(requestBody).contains("max_output_tokens");
        assertThat(requestBody).contains("\"text\"");
        assertThat(requestBody).contains("\"format\"");
        assertThat(requestBody).contains("input_text");
        assertThat(requestBody).doesNotContain("\"type\":\"text\"");
    }

    @Test
    void testContentTruncation() throws Exception {
        String mockResponseJson = """
                {
                    "output_text": [
                        "{\\"summary\\": \\"test\\", \\"skills\\": [], \\"highlights\\": [], \\"structured\\": {}}"
                    ]
                }
                """;
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .setHeader("Content-Type", "application/json"));

        String longContent = "a".repeat(10000); // 超过6000字符的内容
        String longRawContent = "b".repeat(8000); // 超过4000字符的原始内容
        
        provider.enrich(testJob, longRawContent, longContent);
        
        RecordedRequest request = mockWebServer.takeRequest();
        String requestBody = request.getBody().readUtf8();
        
        // 验证内容被截断
        assertThat(requestBody.length()).isLessThan(longContent.length() + longRawContent.length());
    }

    @Test
    void testSkillsAndHighlightsNormalization() throws Exception {
        String mockResponseJson = """
                {
                    "output_text": [
                        "{\\"summary\\": \\"test\\", \\"skills\\": [\\"  Java  \\", \\"Spring\\\\nBoot\\", \\"\\"], \\"highlights\\": [\\"高薪\\\\t待遇\\", \\"  \\"  ], \\"structured\\": {}}"
                    ]
                }
                """;
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .setHeader("Content-Type", "application/json"));

        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, "职位描述");
        
        assertThat(result).isPresent();
        JobContentEnrichment enrichment = result.get();
        
        // 验证技能被正确规范化（去除空白、空字符串）
        assertThat(enrichment.skills()).containsExactly("Java", "Spring Boot");
        
        // 验证亮点被正确规范化
        assertThat(enrichment.highlights()).containsExactly("高薪 待遇");
    }
}
