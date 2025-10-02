package com.vibe.jobs.ingestion;

import com.vibe.jobs.datasource.application.DataSourceQueryService;
import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSource.CategoryQuotaDefinition;
import com.vibe.jobs.datasource.domain.JobDataSource.DataSourceCompany;
import com.vibe.jobs.datasource.domain.PlaceholderContext;
import com.vibe.jobs.datasource.domain.SourceOptionDefaults;
import com.vibe.jobs.sources.SourceClient;
import com.vibe.jobs.sources.SourceClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SourceRegistry {

    private static final Logger log = LoggerFactory.getLogger(SourceRegistry.class);

    private final DataSourceQueryService queryService;
    private final SourceClientFactory factory;
    private final Map<String, SourceClient> clientCache = new ConcurrentHashMap<>();

    public SourceRegistry(DataSourceQueryService queryService, SourceClientFactory factory) {
        this.queryService = queryService;
        this.factory = factory;
    }

    public List<ConfiguredSource> getScheduledSources() {
        return resolveSources(queryService.fetchAllEnabled());
    }

    public List<ConfiguredSource> getStartupSources() {
        return resolveSources(queryService.fetchStartupSources());
    }

    private List<ConfiguredSource> resolveSources(List<JobDataSource> sources) {
        List<ConfiguredSource> resolved = new ArrayList<>();
        for (JobDataSource source : sources) {
            if (source == null || !source.isEnabled()) {
                continue;
            }
            String type = source.getType();
            if (type == null || type.isBlank()) {
                continue;
            }

            if (isSingleInstanceApiClient(type) || !source.supportsCompanies()) {
                resolved.addAll(resolveSingleInstance(source));
            } else {
                resolved.addAll(resolvePerCompany(source));
            }
        }
        return resolved;
    }

    private List<ConfiguredSource> resolveSingleInstance(JobDataSource source) {
        List<ConfiguredSource> result = new ArrayList<>();
        PlaceholderContext context = PlaceholderContext.forCompany(null);
        Map<String, String> options = mergeOptions(source, null, context);
        List<CategoryQuota> categories = resolveCategories(source, context);
        if (options.isEmpty()) {
            return result;
        }
        String cacheKey = cacheKey(source.getCode(), "single");
        try {
            SourceClient client = clientCache.computeIfAbsent(cacheKey, key -> factory.create(source.getType(), options));
            result.add(new ConfiguredSource(source, source.getCode(), client, categories));
        } catch (Exception ex) {
            log.warn("Failed to initialize single-instance source {}: {}", source.getCode(), ex.getMessage());
            log.debug("Source initialization error", ex);
        }
        return result;
    }

    private List<ConfiguredSource> resolvePerCompany(JobDataSource source) {
        List<ConfiguredSource> resolved = new ArrayList<>();
        for (DataSourceCompany company : source.getCompanies()) {
            if (company == null || !company.enabled()) {
                continue;
            }
            PlaceholderContext context = PlaceholderContext.forCompany(company);
            Map<String, String> options = mergeOptions(source, company, context);
            if (options.isEmpty()) {
                continue;
            }
            List<CategoryQuota> categories = resolveCategories(source, context);
            String companyKey = company.reference().isBlank() ? company.displayName() : company.reference();
            String cacheKey = cacheKey(source.getCode(), companyKey);
            try {
                SourceClient client = clientCache.computeIfAbsent(cacheKey, key -> factory.create(source.getType(), options));
                resolved.add(new ConfiguredSource(source, context.company(), client, categories));
            } catch (Exception ex) {
                log.warn("Failed to initialize source {} for company {}: {}", source.getCode(), context.company(), ex.getMessage());
                log.debug("Source initialization error", ex);
            }
        }
        return resolved;
    }

    private String cacheKey(String sourceCode, String company) {
        return (sourceCode == null ? "" : sourceCode) + "::" + (company == null ? "" : company.toLowerCase(Locale.ROOT));
    }

    private Map<String, String> mergeOptions(JobDataSource source,
                                             DataSourceCompany company,
                                             PlaceholderContext context) {
        Map<String, String> merged = new LinkedHashMap<>(source.getBaseOptions());
        merged.replaceAll((k, v) -> applyPlaceholders(v, context));

        Map<String, String> defaults = SourceOptionDefaults.derive(source.getType(), context);
        defaults.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                merged.putIfAbsent(key, value);
            }
        });

        Map<String, String> overrideOptions = company == null ? Map.of() : company.overrideOptions();
        if (source.isRequireOverride() && overrideOptions.isEmpty()) {
            return Map.of();
        }
        overrideOptions.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                merged.put(key, context.apply(value));
            }
        });

        if (!context.company().isBlank()) {
            merged.putIfAbsent("company", context.company());
        }
        if (source.getCode() != null) {
            merged.putIfAbsent("__sourceCode", source.getCode());
            merged.putIfAbsent("__sourceName", "crawler:" + source.getCode());
        }
        if (!context.company().isBlank()) {
            merged.put("__company", context.company());
        }
        merged.replaceAll((k, v) -> applyPlaceholders(v, context));
        merged.values().removeIf(value -> value == null || value.isBlank());
        return merged;
    }

    private List<CategoryQuota> resolveCategories(JobDataSource source, PlaceholderContext context) {
        List<CategoryQuotaDefinition> definitions = source.getCategories();
        if (definitions == null || definitions.isEmpty()) {
            return List.of();
        }
        List<CategoryQuota> result = new ArrayList<>();
        int index = 1;
        for (CategoryQuotaDefinition definition : definitions) {
            if (definition == null || definition.limit() <= 0) {
                continue;
            }
            String name = applyPlaceholders(definition.name(), context);
            if (name == null || name.isBlank()) {
                name = "category-" + index++;
            }
            List<String> tags = definition.tags() == null ? List.of() : definition.tags().stream()
                    .map(tag -> applyPlaceholders(tag, context))
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                    .distinct()
                    .toList();
            Map<String, List<String>> facets = resolveFacets(definition.facets(), context);
            result.add(new CategoryQuota(name, definition.limit(), tags, facets));
        }
        return result;
    }

    private Map<String, List<String>> resolveFacets(Map<String, List<String>> facets, PlaceholderContext context) {
        if (facets == null || facets.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> resolved = new LinkedHashMap<>();
        facets.forEach((key, values) -> {
            String resolvedKey = applyPlaceholders(key, context);
            if (resolvedKey == null || resolvedKey.isBlank()) {
                return;
            }
            List<String> normalizedValues = new ArrayList<>();
            if (values != null) {
                for (String value : values) {
                    String resolvedValue = applyPlaceholders(value, context);
                    if (resolvedValue != null && !resolvedValue.isBlank()) {
                        normalizedValues.add(resolvedValue);
                    }
                }
            }
            if (!normalizedValues.isEmpty()) {
                resolved.put(resolvedKey, List.copyOf(normalizedValues));
            }
        });
        return resolved.isEmpty() ? Map.of() : Map.copyOf(resolved);
    }

    private String applyPlaceholders(String value, PlaceholderContext context) {
        if (value == null) {
            return null;
        }
        return context.apply(value);
    }

    private boolean isSingleInstanceApiClient(String type) {
        if (type == null) {
            return false;
        }
        String normalized = type.toLowerCase(Locale.ROOT);
        return normalized.equals("apple-api") ||
                normalized.equals("microsoft-api") ||
                normalized.equals("amazon-api") ||
                normalized.endsWith("-api");
    }

    public record ConfiguredSource(JobDataSource definition,
                                   String company,
                                   SourceClient client,
                                   List<CategoryQuota> categories) {
        public ConfiguredSource {
            categories = categories == null ? List.of() : List.copyOf(categories);
        }

        public boolean isLimitedFlow() {
            return definition != null && definition.isLimitedFlow();
        }
    }

    public record CategoryQuota(String name,
                                int limit,
                                List<String> tags,
                                Map<String, List<String>> facets) {
        public CategoryQuota {
            tags = tags == null ? List.of() : List.copyOf(tags);
            Map<String, List<String>> normalized = new LinkedHashMap<>();
            if (facets != null) {
                facets.forEach((key, values) -> {
                    List<String> copy = values == null ? List.of() : List.copyOf(values);
                    normalized.put(key, copy);
                });
            }
            facets = normalized.isEmpty() ? Map.of() : Map.copyOf(normalized);
        }

        public boolean hasFacets() {
            return !facets.isEmpty();
        }
    }
}
