package com.vibe.jobs.ingestion;

import com.vibe.jobs.admin.application.IngestionSettingsService;
import com.vibe.jobs.admin.domain.IngestionSettingsSnapshot;
import com.vibe.jobs.admin.domain.event.IngestionSettingsUpdatedEvent;
import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.service.JobDetailService;
import com.vibe.jobs.service.JobService;
import com.vibe.jobs.service.LocationFilterService;
import com.vibe.jobs.service.RoleFilterService;
import com.vibe.jobs.sources.FetchedJob;
import com.vibe.jobs.sources.SourceClient;
import com.vibe.jobs.sources.WorkdaySourceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class JobIngestionScheduler {

    private final JobService jobService;
    private final IngestionProperties ingestionProperties;
    private final SourceRegistry sourceRegistry;
    private final JobIngestionFilter jobFilter;
    private final JobDetailService jobDetailService;
    private final LocationFilterService locationFilterService;
    private final RoleFilterService roleFilterService;
    private final IngestionExecutorManager executorManager;
    private final TaskScheduler taskScheduler;
    private final IngestionSettingsService settingsService;
    private volatile ScheduledFuture<?> scheduledTask;

    public JobIngestionScheduler(JobService jobService,
                                 IngestionProperties ingestionProperties,
                                 SourceRegistry sourceRegistry,
                                 JobIngestionFilter jobFilter,
                                 JobDetailService jobDetailService,
                                 LocationFilterService locationFilterService,
                                 RoleFilterService roleFilterService,
                                 IngestionExecutorManager executorManager,
                                 TaskScheduler taskScheduler,
                                 IngestionSettingsService settingsService) {
        this.jobService = jobService;
        this.ingestionProperties = ingestionProperties;
        this.sourceRegistry = sourceRegistry;
        this.jobFilter = jobFilter;
        this.jobDetailService = jobDetailService;
        this.locationFilterService = locationFilterService;
        this.roleFilterService = roleFilterService;
        this.executorManager = executorManager;
        this.taskScheduler = taskScheduler;
        this.settingsService = settingsService;
        scheduleWith(settingsService.initializeIfNeeded());
    }

    private void scheduleWith(IngestionSettingsSnapshot snapshot) {
        Duration delay = Duration.ofMillis(Math.max(1_000L, snapshot.fixedDelayMs()));
        Instant start = Instant.now().plusMillis(Math.max(0L, snapshot.initialDelayMs()));
        synchronized (this) {
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
            }
            scheduledTask = taskScheduler.scheduleWithFixedDelay(this::runIngestionSafely, start, delay);
            log.info("Scheduled ingestion with initial delay {} ms and fixed delay {} ms", snapshot.initialDelayMs(), snapshot.fixedDelayMs());
        }
    }

    private void runIngestionSafely() {
        try {
            runIngestion();
        } catch (Exception ex) {
            log.error("Scheduled ingestion failed", ex);
        }
    }

    public void runIngestion() {
        List<SourceRegistry.ConfiguredSource> sources = sourceRegistry.getScheduledSources();
        if (sources.isEmpty()) {
            log.info("No ingestion sources configured; skipping scheduled run");
            return;
        }

        int pageSize = Math.max(1, ingestionProperties.getPageSize());
        List<SourceRegistry.ConfiguredSource> limited = new ArrayList<>();
        List<SourceRegistry.ConfiguredSource> unlimited = new ArrayList<>();
        for (SourceRegistry.ConfiguredSource source : sources) {
            if (source.isLimitedFlow()) {
                limited.add(source);
            } else {
                unlimited.add(source);
            }
        }

        CompletableFuture<?>[] tasks = unlimited.stream()
                .map(source -> CompletableFuture.runAsync(() -> processSource(source, pageSize), executorManager.getExecutor()))
                .toArray(CompletableFuture[]::new);

        if (tasks.length > 0) {
            CompletableFuture.allOf(tasks).join();
        }

        for (SourceRegistry.ConfiguredSource source : limited) {
            processSource(source, pageSize);
        }
    }

    private void processSource(SourceRegistry.ConfiguredSource configuredSource, int pageSize) {
        SourceClient sourceClient = configuredSource.client();
        String sourceName = sourceClient.sourceName();
        String companyName = configuredSource.company();
        try {
            List<SourceRegistry.CategoryQuota> categories = configuredSource.categories();
            if (categories == null || categories.isEmpty()) {
                ingestWithoutCategories(sourceClient, sourceName, companyName, pageSize);
            } else {
                ingestWithCategories(configuredSource, pageSize);
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

    private void ingestWithoutCategories(SourceClient sourceClient,
                                         String sourceName,
                                         String companyName,
                                         int pageSize) throws Exception {
        int page = 1;
        while (true) {
            List<FetchedJob> items = sourceClient.fetchPage(page, pageSize);
            if (items == null || items.isEmpty()) {
                break;
            }
            List<FetchedJob> filtered = jobFilter.apply(items);
            List<FetchedJob> locationFiltered = locationFilterService.filterJobs(filtered);
            List<FetchedJob> roleFiltered = roleFilterService.filter(locationFiltered);
            int persisted = storeJobs(roleFiltered);
            if (roleFiltered.isEmpty() || persisted == 0) {
                log.debug("All jobs filtered out for source {} ({}) on page {}", sourceName, companyName, page);
            }
            page++;
        }
    }

    private void ingestWithCategories(SourceRegistry.ConfiguredSource configuredSource,
                                      int pageSize) throws Exception {
        SourceClient client = configuredSource.client();
        Map<SourceRegistry.CategoryQuota, Integer> remaining = initializeRemaining(configuredSource.categories());
        if (remaining.isEmpty()) {
            return;
        }

        if (client instanceof WorkdaySourceClient workday) {
            for (SourceRegistry.CategoryQuota category : configuredSource.categories()) {
                if (!category.hasFacets()) {
                    continue;
                }
                fetchCategoryWithFacets(workday, configuredSource, category, pageSize, remaining);
                if (allQuotasMet(remaining)) {
                    return;
                }
            }
        }

        fetchGenericPages(configuredSource, pageSize, remaining);
    }

    private void fetchGenericPages(SourceRegistry.ConfiguredSource configuredSource,
                                   int pageSize,
                                   Map<SourceRegistry.CategoryQuota, Integer> remaining) throws Exception {
        SourceClient client = configuredSource.client();
        String sourceName = client.sourceName();
        String companyName = configuredSource.company();
        int page = 1;
        while (!allQuotasMet(remaining)) {
            List<FetchedJob> items = client.fetchPage(page, pageSize);
            if (items == null || items.isEmpty()) {
                break;
            }
            List<FetchedJob> filtered = jobFilter.apply(items);
            List<FetchedJob> locationFiltered = locationFilterService.filterJobs(filtered);
            List<FetchedJob> roleFiltered = roleFilterService.filter(locationFiltered);
            int persisted = matchAndStore(roleFiltered, configuredSource, remaining, null, page);
            if (roleFiltered.isEmpty() || persisted == 0) {
                log.debug("No category-matched jobs for source {} ({}) on page {}", sourceName, companyName, page);
            }
            page++;
        }
    }

    private void fetchCategoryWithFacets(WorkdaySourceClient client,
                                         SourceRegistry.ConfiguredSource configuredSource,
                                         SourceRegistry.CategoryQuota category,
                                         int pageSize,
                                         Map<SourceRegistry.CategoryQuota, Integer> remaining) throws Exception {
        String sourceName = client.sourceName();
        String companyName = configuredSource.company();
        int page = 1;
        while (remaining.getOrDefault(category, 0) > 0) {
            List<FetchedJob> items = client.fetchPage(page, pageSize, category.facets());
            if (items == null || items.isEmpty()) {
                break;
            }
            List<FetchedJob> filtered = jobFilter.apply(items);
            List<FetchedJob> locationFiltered = locationFilterService.filterJobs(filtered);
            List<FetchedJob> roleFiltered = roleFilterService.filter(locationFiltered);
            int persisted = matchAndStore(roleFiltered, configuredSource, remaining, category, page);
            if (roleFiltered.isEmpty() || persisted == 0) {
                log.debug("No jobs matched category {} for source {} ({}) on page {} with facets", category.name(), sourceName, companyName, page);
            }
            if (allQuotasMet(remaining)) {
                return;
            }
            page++;
        }
    }

    private Map<SourceRegistry.CategoryQuota, Integer> initializeRemaining(List<SourceRegistry.CategoryQuota> categories) {
        Map<SourceRegistry.CategoryQuota, Integer> remaining = new LinkedHashMap<>();
        if (categories == null) {
            return remaining;
        }
        for (SourceRegistry.CategoryQuota category : categories) {
            if (category == null) {
                continue;
            }
            int limit = Math.max(category.limit(), 0);
            if (limit > 0) {
                remaining.put(category, limit);
            }
        }
        return remaining;
    }

    private boolean allQuotasMet(Map<SourceRegistry.CategoryQuota, Integer> remaining) {
        return remaining.values().stream().allMatch(value -> value != null && value <= 0);
    }

    private int matchAndStore(List<FetchedJob> jobs,
                              SourceRegistry.ConfiguredSource source,
                              Map<SourceRegistry.CategoryQuota, Integer> remaining,
                              SourceRegistry.CategoryQuota targetCategory,
                              int page) {
        if (jobs == null || jobs.isEmpty()) {
            return 0;
        }
        Map<SourceRegistry.CategoryQuota, List<FetchedJob>> grouped = new LinkedHashMap<>();
        for (FetchedJob job : jobs) {
            SourceRegistry.CategoryQuota matched = targetCategory != null
                    ? targetCategory
                    : matchCategory(job.job(), source.categories());
            if (matched == null) {
                continue;
            }
            grouped.computeIfAbsent(matched, key -> new ArrayList<>()).add(job);
        }

        int persisted = 0;
        for (Map.Entry<SourceRegistry.CategoryQuota, List<FetchedJob>> entry : grouped.entrySet()) {
            SourceRegistry.CategoryQuota category = entry.getKey();
            int remainingQuota = remaining.getOrDefault(category, 0);
            if (remainingQuota <= 0) {
                continue;
            }
            List<FetchedJob> matchedJobs = entry.getValue();
            if (matchedJobs.isEmpty()) {
                continue;
            }
            int toPersist = Math.min(remainingQuota, matchedJobs.size());
            List<FetchedJob> subset = matchedJobs.subList(0, toPersist);
            persisted += storeJobs(subset);
            remaining.put(category, remainingQuota - toPersist);
            if (toPersist < matchedJobs.size()) {
                log.debug("Category {} quota reached for {} on page {}", category.name(), source.client().sourceName(), page);
            }
        }
        return persisted;
    }

    private SourceRegistry.CategoryQuota matchCategory(Job job, List<SourceRegistry.CategoryQuota> categories) {
        if (job == null || categories == null || categories.isEmpty()) {
            return null;
        }
        String normalizedTitle = job.getTitle() == null ? "" : job.getTitle().toLowerCase(Locale.ROOT);
        for (SourceRegistry.CategoryQuota category : categories) {
            if (category == null || category.tags() == null) {
                continue;
            }
            for (String tag : category.tags()) {
                if (tag != null && !tag.isBlank() && normalizedTitle.contains(tag)) {
                    return category;
                }
            }
        }
        return null;
    }

    private int storeJobs(List<FetchedJob> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return 0;
        }
        int persisted = 0;
        for (FetchedJob fetched : jobs) {
            try {
                Job persistedJob = jobService.upsert(fetched.job());
                jobDetailService.saveContent(persistedJob, fetched.content());
                persisted++;
            } catch (Exception ex) {
                log.warn("Failed to persist job {} from source {}: {}", fetched.job().getTitle(), fetched.job().getSource(), ex.getMessage());
                log.debug("Job persistence error", ex);
            }
        }
        return persisted;
    }

    @EventListener
    public void handleSettingsUpdated(IngestionSettingsUpdatedEvent event) {
        if (event == null || event.snapshot() == null) {
            return;
        }
        scheduleWith(event.snapshot());
    }
}
