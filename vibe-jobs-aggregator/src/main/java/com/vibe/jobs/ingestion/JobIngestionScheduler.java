package com.vibe.jobs.ingestion;

import com.vibe.jobs.admin.application.IngestionSettingsService;
import com.vibe.jobs.admin.domain.IngestionSettingsSnapshot;
import com.vibe.jobs.admin.domain.event.IngestionSettingsUpdatedEvent;
import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.ingestion.domain.IngestionCursor;
import com.vibe.jobs.ingestion.domain.IngestionCursorKey;
import com.vibe.jobs.service.LocationEnhancementService;
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

    private final IngestionProperties ingestionProperties;
    private final SourceRegistry sourceRegistry;
    private final JobIngestionFilter jobFilter;
    private final JobIngestionPersistenceService persistenceService;
    private final LocationFilterService locationFilterService;
    private final RoleFilterService roleFilterService;
    private final LocationEnhancementService locationEnhancementService;
    private final IngestionExecutorManager executorManager;
    private final TaskScheduler taskScheduler;
    private final IngestionSettingsService settingsService;
    private final IngestionCursorService ingestionCursorService;
    private volatile ScheduledFuture<?> scheduledTask;

    public JobIngestionScheduler(IngestionProperties ingestionProperties,
                                 SourceRegistry sourceRegistry,
                                 JobIngestionFilter jobFilter,
                                 JobIngestionPersistenceService persistenceService,
                                 LocationFilterService locationFilterService,
                                 RoleFilterService roleFilterService,
                                 LocationEnhancementService locationEnhancementService,
                                 IngestionExecutorManager executorManager,
                                 TaskScheduler taskScheduler,
                                 IngestionSettingsService settingsService,
                                 IngestionCursorService ingestionCursorService) {
        this.ingestionProperties = ingestionProperties;
        this.sourceRegistry = sourceRegistry;
        this.jobFilter = jobFilter;
        this.persistenceService = persistenceService;
        this.locationFilterService = locationFilterService;
        this.roleFilterService = roleFilterService;
        this.locationEnhancementService = locationEnhancementService;
        this.executorManager = executorManager;
        this.taskScheduler = taskScheduler;
        this.settingsService = settingsService;
        this.ingestionCursorService = ingestionCursorService;
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
                ingestWithoutCategories(configuredSource, pageSize);
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

    private void ingestWithoutCategories(SourceRegistry.ConfiguredSource configuredSource,
                                         int pageSize) throws Exception {
        SourceClient sourceClient = configuredSource.client();
        String sourceName = sourceClient.sourceName();
        String companyName = configuredSource.company();
        IngestionCursorKey cursorKey = buildCursorKey(configuredSource, null);
        IngestionCursor cursor = ingestionCursorService.find(cursorKey).orElse(null);
        int page = 1;
        while (true) {
            List<FetchedJob> items = sourceClient.fetchPage(page, pageSize);
            if (items == null || items.isEmpty()) {
                break;
            }
            List<FetchedJob> filtered = jobFilter.apply(items);
            List<FetchedJob> locationEnhanced = locationEnhancementService.enhanceLocationFields(filtered);
            List<FetchedJob> locationFiltered = locationFilterService.filterJobs(locationEnhanced);
            List<FetchedJob> roleFiltered = roleFilterService.filter(locationFiltered);
            List<FetchedJob> cursorFiltered = filterByCursor(roleFiltered, cursor);
            if (cursorFiltered.isEmpty()) {
                log.info("No new jobs beyond cursor for source {} ({}) on page {}", sourceName, companyName, page);
                break;
            }
            JobIngestionResult result = storeJobs(cursorFiltered);
            if (result.persisted() <= 0) {
                log.info("All jobs filtered out or skipped for source {} ({}) on page {}", sourceName, companyName, page);
            } else if (result.lastJob() != null) {
                cursor = ingestionCursorService.updatePosition(cursorKey, result.lastJob());
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
        Map<String, IngestionCursor> cursorCache = new LinkedHashMap<>();

        if (client instanceof WorkdaySourceClient workday) {
            for (SourceRegistry.CategoryQuota category : configuredSource.categories()) {
                if (!category.hasFacets()) {
                    continue;
                }
                fetchCategoryWithFacets(workday, configuredSource, category, pageSize, remaining, cursorCache);
                if (allQuotasMet(remaining)) {
                    return;
                }
            }
        }

        fetchGenericPages(configuredSource, pageSize, remaining, cursorCache);
    }

    private void fetchGenericPages(SourceRegistry.ConfiguredSource configuredSource,
                                   int pageSize,
                                   Map<SourceRegistry.CategoryQuota, Integer> remaining,
                                   Map<String, IngestionCursor> cursorCache) throws Exception {
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
            JobIngestionResult result = matchAndStore(roleFiltered, configuredSource, remaining, null, page, cursorCache);
            if (roleFiltered.isEmpty() || result.persisted() == 0) {
                log.debug("No category-matched jobs for source {} ({}) on page {}", sourceName, companyName, page);
            }
            if (!result.advanced()) {
                break;
            }
            page++;
        }
    }

    private void fetchCategoryWithFacets(WorkdaySourceClient client,
                                         SourceRegistry.ConfiguredSource configuredSource,
                                         SourceRegistry.CategoryQuota category,
                                         int pageSize,
                                         Map<SourceRegistry.CategoryQuota, Integer> remaining,
                                         Map<String, IngestionCursor> cursorCache) throws Exception {
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
            JobIngestionResult result = matchAndStore(roleFiltered, configuredSource, remaining, category, page, cursorCache);
            if (roleFiltered.isEmpty() || result.persisted() == 0) {
                log.debug("No jobs matched category {} for source {} ({}) on page {} with facets", category.name(), sourceName, companyName, page);
            }
            if (allQuotasMet(remaining)) {
                return;
            }
            if (!result.advanced()) {
                break;
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

    private JobIngestionResult matchAndStore(List<FetchedJob> jobs,
                                             SourceRegistry.ConfiguredSource source,
                                             Map<SourceRegistry.CategoryQuota, Integer> remaining,
                                             SourceRegistry.CategoryQuota targetCategory,
                                             int page,
                                             Map<String, IngestionCursor> cursorCache) {
        if (jobs == null || jobs.isEmpty()) {
            return JobIngestionResult.empty();
        }
        Map<SourceRegistry.CategoryQuota, List<FetchedJob>> grouped = new LinkedHashMap<>();
        for (FetchedJob job : jobs) {
            if (job == null) {
                continue;
            }
            SourceRegistry.CategoryQuota matched = targetCategory != null
                    ? targetCategory
                    : matchCategory(job.job(), source.categories());
            if (matched == null) {
                continue;
            }
            grouped.computeIfAbsent(matched, key -> new ArrayList<>()).add(job);
        }

        int totalPersisted = 0;
        Job lastJob = null;
        boolean advanced = false;
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
            String cacheKey = category.name() == null ? "" : category.name();
            IngestionCursorKey cursorKey = buildCursorKey(source, cacheKey);
            IngestionCursor cursor = cursorCache.computeIfAbsent(cacheKey, key -> ingestionCursorService.find(cursorKey).orElse(null));
            List<FetchedJob> cursorFiltered = filterByCursor(matchedJobs, cursor);
            if (cursorFiltered.isEmpty()) {
                continue;
            }
            advanced = true;
            int toPersist = Math.min(remainingQuota, cursorFiltered.size());
            List<FetchedJob> subset = cursorFiltered.subList(0, toPersist);
            JobIngestionResult result = storeJobs(subset);
            if (result.persisted() <= 0) {
                continue;
            }
            totalPersisted += result.persisted();
            remaining.put(category, Math.max(0, remainingQuota - result.persisted()));
            if (result.lastJob() != null) {
                IngestionCursor updated = ingestionCursorService.updatePosition(cursorKey, result.lastJob());
                cursorCache.put(cacheKey, updated);
                lastJob = result.lastJob();
            }
            advanced = advanced || result.advanced();
            if (result.persisted() < subset.size()) {
                log.debug("Persisted {} of {} jobs for category {} from source {} on page {}", result.persisted(), subset.size(), category.name(), source.client().sourceName(), page);
            }
        }
        return new JobIngestionResult(totalPersisted, lastJob, advanced);
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

    private List<FetchedJob> filterByCursor(List<FetchedJob> jobs, IngestionCursor cursor) {
        if (jobs == null || jobs.isEmpty()) {
            return new ArrayList<>();
        }
        List<FetchedJob> result = new ArrayList<>();
        if (cursor == null || !cursor.hasPosition()) {
            result.addAll(jobs);
            return result;
        }
        for (FetchedJob job : jobs) {
            if (job == null || job.job() == null) {
                continue;
            }
            if (isAfterCursor(job.job(), cursor)) {
                result.add(job);
            }
        }
        return result;
    }

    private boolean isAfterCursor(Job job, IngestionCursor cursor) {
        if (cursor == null) {
            return true;
        }
        Instant jobPostedAt = job.getPostedAt();
        Instant cursorPostedAt = cursor.lastPostedAt();
        if (jobPostedAt == null || cursorPostedAt == null) {
            return true;
        }
        if (jobPostedAt.isAfter(cursorPostedAt)) {
            return true;
        }
        if (!jobPostedAt.equals(cursorPostedAt)) {
            return false;
        }
        String jobExternalId = job.getExternalId();
        String cursorExternalId = cursor.lastExternalId();
        if (jobExternalId == null || jobExternalId.isBlank()) {
            return true;
        }
        if (cursorExternalId == null || cursorExternalId.isBlank()) {
            return true;
        }
        return jobExternalId.compareTo(cursorExternalId) > 0;
    }

    private IngestionCursorKey buildCursorKey(SourceRegistry.ConfiguredSource source, String categoryName) {
        String sourceCode = source.definition() == null ? "" : source.definition().getCode();
        String sourceName = source.client() == null ? "" : source.client().sourceName();
        String companyName = source.company();
        return IngestionCursorKey.of(sourceCode, sourceName, companyName, categoryName);
    }

    private JobIngestionResult storeJobs(List<FetchedJob> jobs) {
        JobIngestionPersistenceService.JobBatchPersistenceResult result = persistenceService.persistBatch(jobs);
        return new JobIngestionResult(result.persisted(), result.lastJob(), result.advanced());
    }

    private static final class JobIngestionResult {
        private static final JobIngestionResult EMPTY = new JobIngestionResult(0, null, false);
        private final int persisted;
        private final Job lastJob;
        private final boolean advanced;

        private JobIngestionResult(int persisted, Job lastJob, boolean advanced) {
            this.persisted = persisted;
            this.lastJob = lastJob;
            this.advanced = advanced;
        }

        static JobIngestionResult empty() {
            return EMPTY;
        }

        int persisted() {
            return persisted;
        }

        Job lastJob() {
            return lastJob;
        }

        boolean advanced() {
            return advanced;
        }
    }

    @EventListener
    public void handleSettingsUpdated(IngestionSettingsUpdatedEvent event) {
        if (event == null || event.snapshot() == null) {
            return;
        }
        scheduleWith(event.snapshot());
    }
}
