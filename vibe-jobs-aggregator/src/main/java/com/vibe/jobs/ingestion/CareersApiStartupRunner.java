package com.vibe.jobs.ingestion;

import com.vibe.jobs.sources.ArbeitnowSourceClient;
import com.vibe.jobs.domain.Job;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CareersApiStartupRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        ArbeitnowSourceClient client = new ArbeitnowSourceClient();
        try {
            List<Job> jobs = client.fetchPage(1, 20);
            System.out.println("[CareersApiStartupRunner] Arbeitnow 实时职位条数: " + jobs.size());
        } catch (Exception e) {
            System.err.println("[CareersApiStartupRunner] 拉取 Arbeitnow API 失败: " + e.getMessage());
        }
    }
}
