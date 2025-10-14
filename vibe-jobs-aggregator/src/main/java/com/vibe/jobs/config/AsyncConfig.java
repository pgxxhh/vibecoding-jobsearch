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

    private final JobContentEnrichmentExecutorProperties executorProperties;

    public AsyncConfig(JobContentEnrichmentExecutorProperties executorProperties) {
        this.executorProperties = executorProperties;
    }

    @Bean(name = "jobContentEnrichmentExecutor")
    public Executor jobContentEnrichmentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, executorProperties.getCoreSize()));
        executor.setMaxPoolSize(Math.max(executorProperties.getCoreSize(), executorProperties.getMaxSize()));
        executor.setQueueCapacity(Math.max(1, executorProperties.getQueueCapacity()));
        executor.setThreadNamePrefix(executorProperties.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(executorProperties.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(Math.max(0, executorProperties.getAwaitTerminationSeconds()));
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
