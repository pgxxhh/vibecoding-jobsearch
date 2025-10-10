package com.vibe.jobs.ingestion;

import com.vibe.jobs.admin.application.IngestionSettingsService;
import com.vibe.jobs.admin.domain.IngestionSettingsSnapshot;
import com.vibe.jobs.admin.domain.event.IngestionSettingsUpdatedEvent;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class IngestionExecutorManager {

    private static final Logger log = LoggerFactory.getLogger(IngestionExecutorManager.class);

    private final ThreadPoolExecutor executor;

    public IngestionExecutorManager(IngestionSettingsService settingsService) {
        IngestionSettingsSnapshot snapshot = settingsService.initializeIfNeeded();
        int initialConcurrency = Math.max(1, snapshot.concurrency());
        log.info("Initializing ingestion executor with concurrency: {}", initialConcurrency);
        this.executor = createExecutor(initialConcurrency);
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    @EventListener
    public void handleSettingsUpdated(IngestionSettingsUpdatedEvent event) {
        if (event == null || event.snapshot() == null) {
            log.warn("Received null event or snapshot, ignoring settings update");
            return;
        }
        
        int newConcurrency = Math.max(1, event.snapshot().concurrency());
        int currentCorePoolSize = executor.getCorePoolSize();
        int currentMaxPoolSize = executor.getMaximumPoolSize();
        
        if (currentCorePoolSize != newConcurrency) {
            try {
                log.info("Adjusting ingestion executor concurrency from {} to {} (current max: {})", 
                         currentCorePoolSize, newConcurrency, currentMaxPoolSize);
                
                // 安全地调整线程池大小
                if (newConcurrency > currentMaxPoolSize) {
                    // 增加线程数：先设置最大值，再设置核心值
                    executor.setMaximumPoolSize(newConcurrency);
                    executor.setCorePoolSize(newConcurrency);
                } else {
                    // 减少线程数：先设置核心值，再设置最大值
                    executor.setCorePoolSize(newConcurrency);
                    executor.setMaximumPoolSize(newConcurrency);
                }
                
                log.info("Successfully updated executor pool size to {} threads", newConcurrency);
            } catch (Exception e) {
                log.error("Failed to update executor pool size from {} to {}: {}", 
                          currentCorePoolSize, newConcurrency, e.getMessage(), e);
            }
        } else {
            log.debug("Concurrency unchanged at {}, no update needed", newConcurrency);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    private ThreadPoolExecutor createExecutor(int concurrency) {
        int threads = Math.max(1, concurrency);
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
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                threads,
                threads,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                factory
        );
        pool.allowCoreThreadTimeOut(false);
        return pool;
    }
}
