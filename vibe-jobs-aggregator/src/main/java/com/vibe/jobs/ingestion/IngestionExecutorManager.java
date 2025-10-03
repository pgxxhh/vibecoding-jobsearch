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
        this.executor = createExecutor(snapshot.concurrency());
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    @EventListener
    public void handleSettingsUpdated(IngestionSettingsUpdatedEvent event) {
        if (event == null || event.snapshot() == null) {
            return;
        }
        int concurrency = Math.max(1, event.snapshot().concurrency());
        if (executor.getCorePoolSize() != concurrency) {
            log.info("Adjust ingestion executor concurrency from {} to {}", executor.getCorePoolSize(), concurrency);
            executor.setCorePoolSize(concurrency);
            executor.setMaximumPoolSize(concurrency);
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
