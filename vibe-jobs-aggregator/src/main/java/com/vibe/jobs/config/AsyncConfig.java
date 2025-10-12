package com.vibe.jobs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private static final int ENRICHMENT_CORE_POOL_SIZE = 2;
    private static final int ENRICHMENT_MAX_POOL_SIZE = 4;
    private static final int ENRICHMENT_QUEUE_CAPACITY = 20;

    @Bean(name = "jobContentEnrichmentExecutor")
    public Executor jobContentEnrichmentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(ENRICHMENT_CORE_POOL_SIZE);
        executor.setMaxPoolSize(ENRICHMENT_MAX_POOL_SIZE);
        executor.setQueueCapacity(ENRICHMENT_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("job-enrich-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
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
