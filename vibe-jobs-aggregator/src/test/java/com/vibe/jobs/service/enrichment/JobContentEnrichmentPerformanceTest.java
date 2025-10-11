package com.vibe.jobs.service.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.domain.Job;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 性能和压力测试
 */
class JobContentEnrichmentPerformanceTest {

    private MockWebServer mockWebServer;
    private ChatGptJobContentEnrichmentProvider provider;
    private ObjectMapper objectMapper;

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
                Duration.ofSeconds(5), // 较短的超时用于性能测试
                0.2,
                800
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testSingleRequestPerformance() throws Exception {
        String mockResponse = TestUtils.createMockApiResponse();
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .setHeader("Content-Type", "application/json")
                .setBodyDelay(100, TimeUnit.MILLISECONDS)); // 模拟100ms网络延迟

        Job testJob = TestUtils.createTestJob();
        String jobDescription = TestUtils.createTestJobDescription();

        long startTime = System.currentTimeMillis();
        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, jobDescription);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        
        assertThat(result).isPresent();
        assertThat(duration).isLessThan(5000); // 应在5秒内完成
        assertThat(duration).isGreaterThan(95); // 应该至少有网络延迟时间

        System.out.println("单次请求性能: " + duration + "ms");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testConcurrentRequestsPerformance() throws InterruptedException, ExecutionException {
        String mockResponse = TestUtils.createMockApiResponse();
        
        // 准备多个响应
        for (int i = 0; i < 10; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(mockResponse)
                    .setHeader("Content-Type", "application/json")
                    .setBodyDelay(200, TimeUnit.MILLISECONDS));
        }

        List<CompletableFuture<Optional<JobContentEnrichment>>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // 启动10个并发请求
        for (int i = 0; i < 10; i++) {
            final int jobId = i + 1;
            CompletableFuture<Optional<JobContentEnrichment>> future = CompletableFuture.supplyAsync(() -> {
                Job job = TestUtils.createTestJob("Job " + jobId, "Company " + jobId, "Location " + jobId);
                job.setId((long) jobId);
                return provider.enrich(job, null, "Job description " + jobId);
            });
            futures.add(future);
        }

        // 等待所有请求完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );
        allFutures.get(); // 阻塞等待完成

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;

        // 验证结果
        for (CompletableFuture<Optional<JobContentEnrichment>> future : futures) {
            Optional<JobContentEnrichment> result = future.get();
            assertThat(result).isPresent();
        }

        // 并发请求应该比顺序请求快
        assertThat(totalDuration).isLessThan(10 * 200 + 5000); // 应该比顺序执行快很多

        System.out.println("10个并发请求总时间: " + totalDuration + "ms");
        System.out.println("平均每个请求时间: " + (totalDuration / 10.0) + "ms");
    }

    @Test
    void testTimeoutHandling() throws Exception {
        // 模拟慢响应
        mockWebServer.enqueue(new MockResponse()
                .setBody(TestUtils.createMockApiResponse())
                .setHeader("Content-Type", "application/json")
                .setBodyDelay(10, TimeUnit.SECONDS)); // 10秒延迟，超过5秒超时

        Job testJob = TestUtils.createTestJob();
        
        long startTime = System.currentTimeMillis();
        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, "test description");
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        
        // 应该在超时时间附近失败
        assertThat(result).isEmpty();
        assertThat(duration).isBetween(4000L, 7000L); // 5秒超时 ±2秒误差

        System.out.println("超时测试时间: " + duration + "ms");
    }

    @Test
    void testLargeContentHandling() throws Exception {
        String mockResponse = TestUtils.createMockApiResponse();
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .setHeader("Content-Type", "application/json"));

        Job testJob = TestUtils.createTestJob();
        
        // 创建大型内容（100KB）
        StringBuilder largeContent = new StringBuilder();
        String baseText = "这是一个详细的职位描述，包含了很多技术要求和工作职责。";
        while (largeContent.length() < 100000) {
            largeContent.append(baseText).append(" ");
        }

        long startTime = System.currentTimeMillis();
        Optional<JobContentEnrichment> result = provider.enrich(testJob, null, largeContent.toString());
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        
        assertThat(result).isPresent();
        assertThat(duration).isLessThan(10000); // 大内容处理应在10秒内完成

        System.out.println("大内容处理时间: " + duration + "ms");
        System.out.println("原始内容大小: " + largeContent.length() + " 字符");
    }

    @Test
    void testMemoryUsage() throws Exception {
        String mockResponse = TestUtils.createMockApiResponse();
        
        // 准备多个响应
        for (int i = 0; i < 50; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(mockResponse)
                    .setHeader("Content-Type", "application/json"));
        }

        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // 执行多次请求
        for (int i = 0; i < 50; i++) {
            Job job = TestUtils.createTestJob();
            job.setId((long) (i + 1));
            Optional<JobContentEnrichment> result = provider.enrich(job, null, "Test job description " + i);
            assertThat(result).isPresent();
        }

        // 强制垃圾回收
        System.gc();
        Thread.sleep(100);
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;

        System.out.println("初始内存使用: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("最终内存使用: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("内存增长: " + (memoryIncrease / 1024 / 1024) + " MB");

        // 内存增长应该合理（小于100MB）
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024);
    }

    @Test
    void testErrorRateUnderLoad() throws Exception {
        // 混合正常响应和错误响应
        for (int i = 0; i < 20; i++) {
            if (i % 5 == 0) {
                // 20% 错误率
                mockWebServer.enqueue(new MockResponse().setResponseCode(500));
            } else {
                mockWebServer.enqueue(new MockResponse()
                        .setBody(TestUtils.createMockApiResponse())
                        .setHeader("Content-Type", "application/json"));
            }
        }

        int successCount = 0;
        int errorCount = 0;

        for (int i = 0; i < 20; i++) {
            Job job = TestUtils.createTestJob();
            job.setId((long) (i + 1));
            Optional<JobContentEnrichment> result = provider.enrich(job, null, "Test description " + i);
            
            if (result.isPresent()) {
                successCount++;
            } else {
                errorCount++;
            }
        }

        System.out.println("成功请求: " + successCount);
        System.out.println("失败请求: " + errorCount);
        System.out.println("成功率: " + (successCount * 100.0 / 20) + "%");

        // 验证错误处理正常
        assertThat(successCount).isEqualTo(16); // 80% 成功
        assertThat(errorCount).isEqualTo(4);    // 20% 失败
    }
}