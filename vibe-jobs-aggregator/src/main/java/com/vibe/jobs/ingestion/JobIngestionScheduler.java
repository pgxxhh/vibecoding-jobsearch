package com.vibe.jobs.ingestion;

import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.service.JobDetailService;
import com.vibe.jobs.service.JobService;
import com.vibe.jobs.sources.FetchedJob;
import com.vibe.jobs.sources.SourceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class JobIngestionScheduler {

    private final JobService jobService;
    private final IngestionProperties ingestionProperties;
    private final SourceRegistry sourceRegistry;
    private final JobIngestionFilter jobFilter;
    private final JobDetailService jobDetailService;
    private final ExecutorService executor;

    public JobIngestionScheduler(JobService jobService,
                                 IngestionProperties ingestionProperties,
                                 SourceRegistry sourceRegistry,
                                 JobIngestionFilter jobFilter,
                                 JobDetailService jobDetailService,
                                 ExecutorService executor) {
        this.jobService = jobService;
        this.ingestionProperties = ingestionProperties;
        this.sourceRegistry = sourceRegistry;
        this.jobFilter = jobFilter;
        this.jobDetailService = jobDetailService;
        this.executor = executor;
    }

    @Scheduled(fixedDelayString = "${ingestion.fixedDelayMs:3600000}", initialDelayString = "${ingestion.initialDelayMs:10000}")
    public void runIngestion() {
        List<SourceRegistry.ConfiguredSource> sources = sourceRegistry.getScheduledSources();
        if (sources.isEmpty()) {
            log.info("No ingestion sources configured; skipping scheduled run");
            return;
        }

        int pageSize = Math.max(1, ingestionProperties.getPageSize());
        CompletableFuture<?>[] tasks = sources.stream()
                .map(source -> CompletableFuture.runAsync(() -> processSource(source, pageSize), executor))
                .toArray(CompletableFuture[]::new);

        if (tasks.length > 0) {
            CompletableFuture.allOf(tasks).join();
        }
    }

    private void processSource(SourceRegistry.ConfiguredSource configuredSource, int pageSize) {
        SourceClient sourceClient = configuredSource.client();
        String sourceName = sourceClient.sourceName();
        String companyName = configuredSource.company();
        try {
            int page = 1;
            while (true) {
                List<FetchedJob> items = sourceClient.fetchPage(page, pageSize);
                if (items == null || items.isEmpty()) {
                    break;
                }
                List<FetchedJob> filtered = jobFilter.apply(items);
                for (FetchedJob fetched : filtered) {
                    Job persisted = jobService.upsert(fetched.job());
                    jobDetailService.saveContent(persisted, fetched.content());
                }
                if (filtered.isEmpty()) {
                    log.debug("All jobs filtered out for source {} ({}) on page {}", sourceName, companyName, page);
                }
                page++;
            }
            log.info("Ingestion completed for source {} ({})", sourceName, companyName);
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null && message.contains("403")) {
                log.info("Skip source {} ({}) due to 403 response", sourceName, companyName);
                log.debug("Expected failure details", e);
            } else {
                log.warn("Ingestion failed for source {} ({}): {}", sourceName, companyName, message);
                log.debug("Ingestion failure details", e);
            }
        }
    }
}
