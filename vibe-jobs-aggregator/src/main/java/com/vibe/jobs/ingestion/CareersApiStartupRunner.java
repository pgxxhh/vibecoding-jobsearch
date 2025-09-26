package com.vibe.jobs.ingestion;

import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.sources.SourceClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CareersApiStartupRunner implements ApplicationRunner {

    private final SourceRegistry sourceRegistry;
    private final IngestionProperties ingestionProperties;
    private final JobIngestionFilter jobFilter;

    public CareersApiStartupRunner(SourceRegistry sourceRegistry,
                                   IngestionProperties ingestionProperties,
                                   JobIngestionFilter jobFilter) {
        this.sourceRegistry = sourceRegistry;
        this.ingestionProperties = ingestionProperties;
        this.jobFilter = jobFilter;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<SourceRegistry.ConfiguredSource> startupSources = sourceRegistry.getStartupSources();
        if (startupSources.isEmpty()) {
            System.out.println("[CareersApiStartupRunner] 未配置启动时拉取的数据源");
            return;
        }

        int pageSize = Math.max(1, ingestionProperties.getPageSize());

        startupSources.forEach(source -> {
            SourceClient client = source.client();
            String companyName = source.company();
            try {
                List<Job> jobs = client.fetchPage(1, pageSize);
                List<Job> filtered = jobFilter.apply(jobs);
                System.out.println("[CareersApiStartupRunner] " + client.sourceName()
                        + "(" + (companyName == null ? "unknown" : companyName) + ") 首批职位数量: "
                        + (filtered == null ? 0 : filtered.size()));
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null && message.contains("403")) {
                    System.out.println("[CareersApiStartupRunner] 跳过 " + client.sourceName()
                            + "(" + (companyName == null ? "unknown" : companyName) + ")，因为远端返回 403");
                } else {
                    System.err.println("[CareersApiStartupRunner] 拉取 " + client.sourceName()
                            + "(" + (companyName == null ? "unknown" : companyName) + ") 失败: " + message);
                }
            }
        });
    }
}
