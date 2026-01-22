package com.vibe.jobs.ingestion;

import com.vibe.jobs.admin.domain.event.DataSourceConfigurationChangedEvent;
import com.vibe.jobs.datasource.application.DataSourceQueryService;
import com.vibe.jobs.ingestion.infrastructure.sourceclient.FetchedJob;
import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class JobIngestionFilter {

    private final IngestionProperties properties;
    private final DataSourceQueryService queryService;
    private static final Duration ENABLED_COMPANIES_CACHE_TTL = Duration.ofMinutes(5);
    private final AtomicReference<CompanyWhitelist> enabledCompaniesCache = new AtomicReference<>(CompanyWhitelist.empty());
    private final AtomicReference<Instant> enabledCompaniesLastRefresh = new AtomicReference<>(null);

    public JobIngestionFilter(IngestionProperties properties, DataSourceQueryService queryService) {
        this.properties = properties;
        this.queryService = queryService;
    }

    public List<FetchedJob> apply(List<FetchedJob> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return List.of();
        }

        // 1. 首先按启用的公司过滤 (必须条件)
        CompanyWhitelist whitelist = getCompanyWhitelist();

        // 2. 按时间范围过滤 (必须条件)
        Instant cutoff = Instant.now().minus(Duration.ofDays(Math.max(properties.getRecentDays(), 1)));

        return jobs.stream()
                .filter(job -> matchesEnabledCompany(job.job(), whitelist))
                .filter(job -> isRecent(job.job(), cutoff))
                .toList();
    }

    private boolean matchesEnabledCompany(Job job, CompanyWhitelist whitelist) {
        if (job == null) return false;
        String company = job.getCompany();
        if (company == null) return false;

        String normalized = company.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        
        // 如果没有启用的公司，则跳过公司过滤
        if (whitelist.global().isEmpty()) {
            return true;
        }
        
        if (whitelist.global().contains(normalized)) {
            return true;
        }
        
        String source = job.getSource() == null ? "" : job.getSource().trim().toLowerCase(Locale.ROOT);
        if (source.isBlank()) {
            return false;
        }

        Set<String> companiesForSource = whitelist.perSource().get(source);
        if (companiesForSource == null || companiesForSource.isEmpty()) {
            // 数据源未配置公司白名单，放行
            return true;
        }
        
        return companiesForSource.contains(normalized);
    }

    private boolean isRecent(Job job, Instant cutoff) {
        if (job == null) return false;
        Instant postedAt = job.getPostedAt();
        if (postedAt == null) {
            return true; // 如果时间戳未知，保留职位
        }
        return !postedAt.isBefore(cutoff);
    }

    private CompanyWhitelist getCompanyWhitelist() {
        Instant now = Instant.now();
        CompanyWhitelist cached = enabledCompaniesCache.get();
        Instant lastRefresh = enabledCompaniesLastRefresh.get();
        if (isCacheValid(cached, lastRefresh, now)) {
            return cached;
        }

        synchronized (this) {
            now = Instant.now();
            cached = enabledCompaniesCache.get();
            lastRefresh = enabledCompaniesLastRefresh.get();
            if (isCacheValid(cached, lastRefresh, now)) {
                return cached;
            }

            Set<String> fetched = queryService.getNormalizedCompanyNames();
            Map<String, Set<String>> perSource = queryService.getNormalizedCompaniesBySource();
            Set<String> normalized = fetched == null ? Set.of() : Set.copyOf(fetched);
            CompanyWhitelist updated = new CompanyWhitelist(normalized, normalizePerSource(perSource));
            enabledCompaniesCache.set(updated);
            enabledCompaniesLastRefresh.set(now);
            return updated;
        }
    }

    private Map<String, Set<String>> normalizePerSource(Map<String, Set<String>> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Set<String>> normalized = new HashMap<>();
        raw.forEach((key, values) -> {
            if (key == null || key.isBlank() || values == null || values.isEmpty()) {
                return;
            }
            Set<String> cleaned = values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> value.trim().toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toSet());
            normalized.put(key.trim().toLowerCase(Locale.ROOT), cleaned);
        });
        return normalized;
    }

    private boolean isCacheValid(CompanyWhitelist cached, Instant lastRefresh, Instant now) {
        if (lastRefresh == null || cached == null) {
            return false;
        }
        return Duration.between(lastRefresh, now).compareTo(ENABLED_COMPANIES_CACHE_TTL) < 0;
    }

    @EventListener
    public void onDataSourceConfigurationChanged(DataSourceConfigurationChangedEvent event) {
        clearEnabledCompaniesCache();
    }

    private void clearEnabledCompaniesCache() {
        enabledCompaniesCache.set(CompanyWhitelist.empty());
        enabledCompaniesLastRefresh.set(null);
    }

    private record CompanyWhitelist(Set<String> global, Map<String, Set<String>> perSource) {
        static CompanyWhitelist empty() {
            return new CompanyWhitelist(Set.of(), Map.of());
        }
    }
}
