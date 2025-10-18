package com.vibe.jobs.shared.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class IngestionExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService ingestionExecutorService(IngestionProperties properties) {
        int threads = Math.max(1, properties.getConcurrency());
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("ingestion-worker-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
        return Executors.newFixedThreadPool(threads, factory);
    }
}
