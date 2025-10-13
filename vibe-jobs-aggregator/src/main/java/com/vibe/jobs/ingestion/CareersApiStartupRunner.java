package com.vibe.jobs.ingestion;

import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.service.JobDetailService;
import com.vibe.jobs.service.JobService;
import com.vibe.jobs.service.LocationFilterService;
import com.vibe.jobs.service.RoleFilterService;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.sources.SourceClient;
import com.vibe.jobs.sources.FetchedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class CareersApiStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CareersApiStartupRunner.class);

    private final SourceRegistry sourceRegistry;
    private final IngestionProperties ingestionProperties;
    private final JobIngestionFilter jobFilter;
    private final LocationFilterService locationFilterService;
    private final RoleFilterService roleFilterService;
    private final JobService jobService;
    private final JobDetailService jobDetailService;
    private final IngestionExecutorManager executorManager;

    public CareersApiStartupRunner(SourceRegistry sourceRegistry,
                                   IngestionProperties ingestionProperties,
                                   JobIngestionFilter jobFilter,
                                   LocationFilterService locationFilterService,
                                   RoleFilterService roleFilterService,
                                   JobService jobService,
                                   JobDetailService jobDetailService,
                                   IngestionExecutorManager executorManager) {
        this.sourceRegistry = sourceRegistry;
        this.ingestionProperties = ingestionProperties;
        this.jobFilter = jobFilter;
        this.locationFilterService = locationFilterService;
        this.roleFilterService = roleFilterService;
        this.jobService = jobService;
        this.jobDetailService = jobDetailService;
        this.executorManager = executorManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 打印location过滤器状态
        log.info("Location filter status: {}", locationFilterService.getFilterStatus());
        log.info("Role filter status: {}", roleFilterService.getFilterStatus());
        
        List<SourceRegistry.ConfiguredSource> startupSources = sourceRegistry.getStartupSources();
        if (startupSources.isEmpty()) {
            log.warn("No startup sources configured; skipping initial fetch");
            return;
        }

        int pageSize = Math.max(1, ingestionProperties.getPageSize());
        List<SourceRegistry.ConfiguredSource> limited = new ArrayList<>();
        List<SourceRegistry.ConfiguredSource> unlimited = new ArrayList<>();
        for (SourceRegistry.ConfiguredSource source : startupSources) {
            if (source.isLimitedFlow()) {
                limited.add(source);
            } else {
                unlimited.add(source);
            }
        }

        ExecutorService executor = executorManager.getExecutor();
        CompletableFuture<?>[] tasks = unlimited.stream()
                .map(source -> CompletableFuture.runAsync(() -> fetchOnce(source, pageSize), executor))
                .toArray(CompletableFuture[]::new);

        if (tasks.length > 0) {
            CompletableFuture.allOf(tasks).join();
        }

        for (SourceRegistry.ConfiguredSource source : limited) {
            fetchOnce(source, pageSize);
        }
    }

    private void fetchOnce(SourceRegistry.ConfiguredSource source, int pageSize) {
        SourceClient client = source.client();
        String companyName = source.company();
        try {
            List<FetchedJob> jobs = client.fetchPage(1, pageSize);
            List<FetchedJob> filtered = jobFilter.apply(jobs);
            List<FetchedJob> locationFiltered = locationFilterService.filterJobs(filtered);
            List<FetchedJob> roleFiltered = roleFilterService.filter(locationFiltered);
            log.info("{}({}) first page job count after filters: {}", client.sourceName(),
                    companyName == null ? "unknown" : companyName,
                    roleFiltered == null ? 0 : roleFiltered.size());
            if (roleFiltered != null) {
                roleFiltered.forEach(fetched -> {
                    Job persisted = jobService.upsert(fetched.job());
                    jobDetailService.saveContent(persisted, fetched.content());
                });
            }
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null && message.contains("403")) {
                log.warn("Skipping {}({}) because remote returned 403", client.sourceName(),
                        companyName == null ? "unknown" : companyName);
            } else {
                log.error("Failed to fetch jobs for {}({}): {}", client.sourceName(),
                        companyName == null ? "unknown" : companyName, message, e);
            }
        }
    }
}
