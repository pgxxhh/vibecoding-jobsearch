package com.vibe.jobs.shared.infrastructure.config;

import com.vibe.jobs.crawler.infrastructure.config.CrawlerBlueprintGenerationExecutorProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private final JobContentEnrichmentExecutorProperties enrichmentExecutorProperties;
    private final CrawlerBlueprintGenerationExecutorProperties blueprintExecutorProperties;

    public AsyncConfig(JobContentEnrichmentExecutorProperties executorProperties,
                      CrawlerBlueprintGenerationExecutorProperties blueprintExecutorProperties) {
        this.enrichmentExecutorProperties = executorProperties;
        this.blueprintExecutorProperties = blueprintExecutorProperties;
    }

    @Bean(name = "jobContentEnrichmentExecutor")
    public Executor jobContentEnrichmentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, enrichmentExecutorProperties.getCoreSize()));
        executor.setMaxPoolSize(Math.max(enrichmentExecutorProperties.getCoreSize(), enrichmentExecutorProperties.getMaxSize()));
        executor.setQueueCapacity(Math.max(1, enrichmentExecutorProperties.getQueueCapacity()));
        executor.setThreadNamePrefix(enrichmentExecutorProperties.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(enrichmentExecutorProperties.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(Math.max(0, enrichmentExecutorProperties.getAwaitTerminationSeconds()));
        executor.initialize();
        return executor;
    }

    @Bean(name = "crawlerBlueprintGenerationExecutor")
    public Executor crawlerBlueprintGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, blueprintExecutorProperties.getCoreSize()));
        executor.setMaxPoolSize(Math.max(blueprintExecutorProperties.getCoreSize(), blueprintExecutorProperties.getMaxSize()));
        executor.setQueueCapacity(Math.max(1, blueprintExecutorProperties.getQueueCapacity()));
        executor.setThreadNamePrefix(blueprintExecutorProperties.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(blueprintExecutorProperties.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(Math.max(0, blueprintExecutorProperties.getAwaitTerminationSeconds()));
        executor.initialize();
        return executor;
    }

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-sender-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
