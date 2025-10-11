package com.vibe.jobs.service.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.domain.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 集成测试 - 需要真实的OpenAI API密钥
 * 使用环境变量 OPENAI_API_KEY 来启用此测试
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
class JobContentEnrichmentIntegrationTest {

    @Autowired
    private JobContentEnrichmentClient enrichmentClient;

    @Autowired
    private ObjectMapper objectMapper;

    private Job testJob;

    @BeforeEach
    void setUp() {
        testJob = new Job();
        testJob.setId(1L);
        testJob.setTitle("高级Java开发工程师");
        testJob.setCompany("字节跳动");
        testJob.setLocation("北京");
    }

    @Test
    void testRealApiIntegration() {
        String jobDescription = """
                岗位职责：
                1. 负责后端服务架构设计和开发
                2. 参与核心业务系统的技术选型和实现
                3. 优化系统性能，提升用户体验
                4. 指导初级开发人员，进行代码审查
                
                任职要求：
                1. 计算机相关专业本科及以上学历
                2. 3年以上Java开发经验
                3. 熟练掌握Spring Boot、MyBatis、Redis等技术
                4. 有分布式系统开发经验
                5. 具备良好的沟通协调能力
                
                福利待遇：
                - 薪资范围：20k-35k
                - 五险一金，补充医疗保险
                - 年终奖13-16薪
                - 弹性工作时间，技术培训机会
                """;

        Optional<JobContentEnrichment> result = enrichmentClient.enrich(testJob, null, jobDescription);

        assertThat(result).isPresent();
        JobContentEnrichment enrichment = result.get();

        // 验证摘要
        assertThat(enrichment.summary()).isNotBlank();
        assertThat(enrichment.summary().length()).isLessThanOrEqualTo(200);

        // 验证技能提取
        assertThat(enrichment.skills()).isNotEmpty();
        assertThat(enrichment.skills().size()).isBetween(3, 8);
        assertThat(enrichment.skills()).anyMatch(skill -> 
                skill.toLowerCase().contains("java") || 
                skill.toLowerCase().contains("spring"));

        // 验证亮点提取
        assertThat(enrichment.highlights()).isNotEmpty();

        // 验证结构化数据
        assertThat(enrichment.structuredData()).isNotBlank();
        
        System.out.println("=== 集成测试结果 ===");
        System.out.println("摘要: " + enrichment.summary());
        System.out.println("技能: " + enrichment.skills());
        System.out.println("亮点: " + enrichment.highlights());
        System.out.println("结构化数据: " + enrichment.structuredData());
    }

    @Test
    void testRealApiWithHtmlContent() {
        String htmlContent = """
                <div class="job-detail">
                    <h2>Python后端开发工程师</h2>
                    <p><strong>工作职责：</strong></p>
                    <ul>
                        <li>负责Python后端服务开发</li>
                        <li>参与微服务架构设计</li>
                        <li>数据库设计和优化</li>
                    </ul>
                    <p><strong>任职要求：</strong></p>
                    <ul>
                        <li>3年以上Python开发经验</li>
                        <li>熟悉Django/Flask框架</li>
                        <li>熟悉MySQL、Redis</li>
                    </ul>
                </div>
                """;

        String textContent = """
                Python后端开发工程师
                工作职责：
                - 负责Python后端服务开发
                - 参与微服务架构设计  
                - 数据库设计和优化
                任职要求：
                - 3年以上Python开发经验
                - 熟悉Django/Flask框架
                - 熟悉MySQL、Redis
                """;

        testJob.setTitle("Python后端开发工程师");

        Optional<JobContentEnrichment> result = enrichmentClient.enrich(testJob, htmlContent, textContent);

        assertThat(result).isPresent();
        JobContentEnrichment enrichment = result.get();

        assertThat(enrichment.summary()).isNotBlank();
        assertThat(enrichment.skills()).anyMatch(skill -> 
                skill.toLowerCase().contains("python") || 
                skill.toLowerCase().contains("django") ||
                skill.toLowerCase().contains("flask"));

        System.out.println("\n=== HTML内容测试结果 ===");
        System.out.println("摘要: " + enrichment.summary());
        System.out.println("技能: " + enrichment.skills());
    }

    @Test
    void testRealApiPerformance() {
        String jobDescription = "简单的Java开发职位，要求有Spring经验。";

        long startTime = System.currentTimeMillis();
        Optional<JobContentEnrichment> result = enrichmentClient.enrich(testJob, null, jobDescription);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        
        assertThat(result).isPresent();
        assertThat(duration).isLessThan(30000); // 应在30秒内完成

        System.out.println("\n=== 性能测试结果 ===");
        System.out.println("响应时间: " + duration + "ms");
    }

    @Test
    void testRealApiWithLongContent() {
        StringBuilder longDescription = new StringBuilder();
        longDescription.append("详细的职位描述：\n");
        for (int i = 0; i < 100; i++) {
            longDescription.append("第").append(i + 1).append("条要求：需要具备相关的技术技能和工作经验。\n");
        }
        longDescription.append("核心技能包括Java、Spring Boot、MySQL、Redis等。");

        Optional<JobContentEnrichment> result = enrichmentClient.enrich(testJob, null, longDescription.toString());

        assertThat(result).isPresent();
        JobContentEnrichment enrichment = result.get();

        // 即使是长内容，也应该能正确提取关键信息
        assertThat(enrichment.summary()).isNotBlank();
        assertThat(enrichment.skills()).isNotEmpty();

        System.out.println("\n=== 长内容测试结果 ===");
        System.out.println("原始内容长度: " + longDescription.length());
        System.out.println("提取的技能: " + enrichment.skills());
    }
}