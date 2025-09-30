package com.vibe.jobs.ingestion;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.HashSet;
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
    private final LocationFilterService locationFilterService;
    private final RoleFilterService roleFilterService;
    private final ExecutorService executor;

    public JobIngestionScheduler(JobService jobService,
                                 IngestionProperties ingestionProperties,
                                 SourceRegistry sourceRegistry,
                                 JobIngestionFilter jobFilter,
                                 JobDetailService jobDetailService,
                                 LocationFilterService locationFilterService,
                                 RoleFilterService roleFilterService,
                                 ExecutorService executor) {
        this.jobService = jobService;
        this.ingestionProperties = ingestionProperties;
        this.sourceRegistry = sourceRegistry;
        this.jobFilter = jobFilter;
        this.jobDetailService = jobDetailService;
        this.locationFilterService = locationFilterService;
        this.roleFilterService = roleFilterService;
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
        if (remaining.isEmpty()) {
            return true;
        }
        return remaining.values().stream().allMatch(value -> value != null && value <= 0);
    }

    private int matchAndStore(List<FetchedJob> jobs,
                              SourceRegistry.ConfiguredSource configuredSource,
                              Map<SourceRegistry.CategoryQuota, Integer> remaining,
                              SourceRegistry.CategoryQuota preferredCategory,
                              int page) {
        if (jobs == null || jobs.isEmpty()) {
            return 0;
        }
        String sourceName = configuredSource.client().sourceName();
        String companyName = configuredSource.company();
        int stored = 0;
        for (FetchedJob job : jobs) {
            SourceRegistry.CategoryQuota matched = findMatchingCategory(job, configuredSource.categories(), remaining, preferredCategory);
            if (matched == null) {
                continue;
            }
            storeJob(job);
            remaining.computeIfPresent(matched, (key, value) -> value == null ? 0 : Math.max(0, value - 1));
            stored++;
            if (allQuotasMet(remaining)) {
                break;
            }
        }
        if (stored == 0) {
            log.debug("Jobs fetched for source {} ({}) on page {} did not match remaining category quotas", sourceName, companyName, page);
        }
        return stored;
    }

    private SourceRegistry.CategoryQuota findMatchingCategory(FetchedJob fetched,
                                                              List<SourceRegistry.CategoryQuota> categories,
                                                              Map<SourceRegistry.CategoryQuota, Integer> remaining,
                                                              SourceRegistry.CategoryQuota preferredCategory) {
        if (categories == null || categories.isEmpty() || fetched == null || fetched.job() == null) {
            return null;
        }
        Set<String> jobTags = normalizeTags(fetched.job().getTags());
        if (preferredCategory != null) {
            if (remaining.getOrDefault(preferredCategory, 0) > 0 && matchesCategory(preferredCategory, jobTags)) {
                return preferredCategory;
            }
            return null;
        }
        for (SourceRegistry.CategoryQuota category : categories) {
            if (category == null) {
                continue;
            }
            if (remaining.getOrDefault(category, 0) <= 0) {
                continue;
            }
            if (matchesCategory(category, jobTags)) {
                return category;
            }
        }
        return null;
    }

    private boolean matchesCategory(SourceRegistry.CategoryQuota category, Set<String> jobTags) {
        List<String> requiredTags = category.tags();
        if (requiredTags == null || requiredTags.isEmpty()) {
            return true;
        }
        for (String tag : requiredTags) {
            if (jobTags.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> normalizeTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String trimmed = tag.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            normalized.add(trimmed.toLowerCase(Locale.ROOT));
        }
        return normalized;
    }

    private int storeJobs(List<FetchedJob> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (FetchedJob fetched : jobs) {
            storeJob(fetched);
            count++;
        }
        return count;
    }

    private void storeJob(FetchedJob fetched) {
        if (fetched == null) {
            return;
        }
        Job persisted = jobService.upsert(fetched.job());
        jobDetailService.saveContent(persisted, fetched.content());
    }
}
