package com.vibe.jobs.service.enrichment;

import com.vibe.jobs.domain.Job;

public class TestUtils {
    
    public static Job createTestJob() {
        Job job = new Job();
        job.setId(1L);
        job.setTitle("Java开发工程师");
        job.setCompany("测试公司");
        job.setLocation("北京");
        return job;
    }
    
    public static Job createTestJob(String title, String company, String location) {
        Job job = new Job();
        job.setId(1L);
        job.setTitle(title);
        job.setCompany(company);
        job.setLocation(location);
        return job;
    }
    
    public static String createTestJobDescription() {
        return """
                岗位职责：
                1. 负责Java后端开发
                2. 参与系统架构设计
                3. 代码审查和技术指导
                
                任职要求：
                1. 3年以上Java开发经验
                2. 熟练掌握Spring Boot、MyBatis
                3. 熟悉MySQL、Redis等技术
                4. 有分布式系统经验优先
                
                福利待遇：
                - 薪资：15k-25k
                - 五险一金
                - 年终奖
                - 技术培训
                """;
    }
    
    public static String createMockApiResponse() {
        return """
                {
                    "output_text": [
                        "{\\"summary\\": \\"负责Java后端开发，参与系统架构设计，要求3年以上经验\\", \\"skills\\": [\\"Java\\", \\"Spring Boot\\", \\"MyBatis\\", \\"MySQL\\", \\"Redis\\"], \\"highlights\\": [\\"五险一金\\", \\"年终奖\\", \\"技术培训\\"], \\"structured\\": {\\"salary\\": \\"15k-25k\\", \\"experienceLevel\\": \\"3年以上\\", \\"employmentType\\": \\"全职\\"}}"
                    ]
                }
                """;
    }
    
    public static String createMockApiResponseWithOutput() {
        return """
                {
                    "output": [
                        {
                            "type": "message",
                            "role": "assistant",
                            "content": [
                                {
                                    "type": "text",
                                    "text": "{\\"summary\\": \\"高级Java开发职位\\", \\"skills\\": [\\"Java\\", \\"Spring\\"], \\"highlights\\": [\\"高薪\\"], \\"structured\\": {}}"
                                }
                            ]
                        }
                    ]
                }
                """;
    }
}