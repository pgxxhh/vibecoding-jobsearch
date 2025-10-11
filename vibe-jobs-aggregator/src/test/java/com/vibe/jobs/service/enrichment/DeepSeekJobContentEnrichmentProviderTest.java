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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DeepSeekJobContentEnrichmentProviderTest {

    private MockWebServer mockWebServer;
    private DeepSeekJobContentEnrichmentProvider provider;
    private ObjectMapper objectMapper;
    private Job testJob;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();

        String baseUrl = mockWebServer.url("/").toString();
        provider = new DeepSeekJobContentEnrichmentProvider(
                objectMapper,
                "test-api-key",
                baseUrl,
                "/chat/completions",
                "deepseek-chat",
                Duration.ofSeconds(10),
                0.2,
                800
        );

        testJob = new Job();
        testJob.setId(1L);
        testJob.setTitle("Java 开发工程师");
        testJob.setCompany("技术公司");
        testJob.setLocation("上海");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testProviderName() {
        assertThat(provider.name()).isEqualTo("deepseek");
    }

    @Test
    void testIsEnabledWithApiKey() {
        assertThat(provider.isEnabled()).isTrue();
    }

    @Test
    void testIsEnabledWithoutApiKey() {
        DeepSeekJobContentEnrichmentProvider providerWithoutKey = new DeepSeekJobContentEnrichmentProvider(
                objectMapper, "", "https://api.deepseek.com", "/chat/completions", "deepseek-chat",
                Duration.ofSeconds(10), 0.2, 800
        );
        assertThat(providerWithoutKey.isEnabled()).isFalse();
    }

    @Test
    void testSuccessfulEnrichment() throws Exception {
        String mockResponseJson = """
                {
                    "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "{\\"summary\\": \\"负责后端服务开发\\", \\"skills\\": [\\"Java\\", \\"Spring Boot\\"], \\"highlights\\": [\\"弹性工作\\"], \\"structured\\": {\\"salary\\": \\"20k-30k\\"}}"
                            }
                        }
                    ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .setHeader("Content-Type", "application/json"));

        Optional<JobContentEnrichment> result = provider.enrich(testJob, "<html>原始HTML内容</html>", "职位描述纯文本");

        assertThat(result).isPresent();
        JobContentEnrichment enrichment = result.get();

        assertThat(enrichment.summary()).isEqualTo("负责后端服务开发");
        assertThat(enrichment.skills()).containsExactly("Java", "Spring Boot");
        assertThat(enrichment.highlights()).containsExactly("弹性工作");
        assertThat(enrichment.structuredData()).isNotNull();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/chat/completions");
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-api-key");
        assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json");
        String requestBody = recordedRequest.getBody().readUtf8();
        assertThat(requestBody).contains("\"response_format\"");
        assertThat(requestBody).contains("job_detail_enrichment");
    }

    @Test
    void testEnrichmentWithInvalidJson() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("invalid")
                .setHeader("Content-Type", "application/json"));

        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, "职位描述");

        assertThat(result).isEmpty();
    }

    @Test
    void testEnrichmentWithHttpError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, "职位描述");

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
                    "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "{\\"summary\\": \\"test\\", \\"skills\\": [], \\"highlights\\": [], \\"structured\\": {}}"
                            }
                        }
                    ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .setHeader("Content-Type", "application/json"));

        provider.enrich(testJob, "<p>原始HTML</p>", "纯文本内容");

        RecordedRequest request = mockWebServer.takeRequest();
        String requestBody = request.getBody().readUtf8();

        assertThat(requestBody).contains("deepseek-chat");
        assertThat(requestBody).contains("Java 开发工程师");
        assertThat(requestBody).contains("技术公司");
        assertThat(requestBody).contains("上海");
        assertThat(requestBody).contains("纯文本内容");
        assertThat(requestBody).contains("原始HTML");
        assertThat(requestBody).contains("\"response_format\"");
        assertThat(requestBody).contains("\"json_schema\"");
        assertThat(requestBody).contains("job_detail_enrichment");
    }

    @Test
    void testSkillsNormalization() throws Exception {
        String mockResponseJson = """
                {
                    "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "{\\"summary\\": \\"test\\", \\"skills\\": [\\"  Java  \\", \\"Spring\\\\nBoot\\"], \\"highlights\\": [\\" 高薪 \\"], \\"structured\\": {}}"
                            }
                        }
                    ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .setHeader("Content-Type", "application/json"));

        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, "职位描述");

        assertThat(result).isPresent();
        assertThat(result.get().skills()).containsExactly("Java", "Spring Boot");
        assertThat(result.get().highlights()).containsExactly("高薪");
    }
}
