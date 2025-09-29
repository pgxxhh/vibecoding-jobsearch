package com.vibe.jobs.ingestion;

import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.sources.SourceClient;
import com.vibe.jobs.sources.SourceClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SourceRegistry {

    private static final Logger log = LoggerFactory.getLogger(SourceRegistry.class);

    private final IngestionProperties properties;
    private final SourceClientFactory factory;
    private final Map<String, SourceClient> clientCache = new ConcurrentHashMap<>();

    public SourceRegistry(IngestionProperties properties, SourceClientFactory factory) {
        this.properties = properties;
        this.factory = factory;
    }

    public List<ConfiguredSource> getScheduledSources() {
        return resolveSources(false);
    }

    public List<ConfiguredSource> getStartupSources() {
        return resolveSources(true);
    }

    private List<ConfiguredSource> resolveSources(boolean startupOnly) {
        List<ConfiguredSource> resolved = new ArrayList<>();
        for (IngestionProperties.Source source : properties.getSources()) {
            if (!source.isEnabled()) {
                continue;
            }
            if (startupOnly && !source.isRunOnStartup()) {
                continue;
            }

            String type = source.getType();
            if (type == null || type.isBlank()) {
                continue;
            }

            for (String companyName : properties.getCompanies()) {
                if (companyName == null || companyName.isBlank()) {
                    continue;
                }
                String trimmedCompany = companyName.trim();
                Map<String, String> mergedOptions = mergeOptions(type, source, trimmedCompany);
                if (mergedOptions.isEmpty()) {
                    continue;
                }
                String cacheKey = source.key() + "::" + trimmedCompany;
                List<CategoryQuota> categories = resolveCategories(source, trimmedCompany);
                try {
                    SourceClient client = clientCache.computeIfAbsent(cacheKey, key -> factory.create(type, mergedOptions));
                    resolved.add(new ConfiguredSource(source, trimmedCompany, client, categories));
                } catch (Exception ex) {
                    log.warn("Failed to initialize source {} for company {}: {}", source.displayName(), trimmedCompany, ex.getMessage());
                    log.debug("Source initialization error", ex);
                }
            }
        }
        return resolved;
    }

    public record ConfiguredSource(IngestionProperties.Source definition,
                                   String company,
                                   SourceClient client,
                                   List<CategoryQuota> categories) {
        public ConfiguredSource {
            categories = categories == null ? List.of() : List.copyOf(categories);
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

    private Map<String, String> mergeOptions(String type,
                                             IngestionProperties.Source source,
                                             String companyName) {
        Map<String, String> result = new HashMap<>(source.getOptions());
        result.replaceAll((k, v) -> applyPlaceholders(v, companyName));

        Map<String, String> derived = deriveDefaults(type, companyName);
        derived.forEach(result::putIfAbsent);

        if (companyName != null && !companyName.isBlank()) {
            result.putIfAbsent("company", companyName);
        }

        result.replaceAll((k, v) -> applyPlaceholders(v, companyName));

        return result;
    }

    private List<CategoryQuota> resolveCategories(IngestionProperties.Source source, String companyName) {
        List<IngestionProperties.Source.CategoryQuota> configured = source.getCategories();
        if (configured == null || configured.isEmpty()) {
            return List.of();
        }
        List<CategoryQuota> result = new ArrayList<>();
        for (IngestionProperties.Source.CategoryQuota quota : configured) {
            if (quota == null) {
                continue;
            }
            int limit = Math.max(quota.getLimit(), 0);
            if (limit <= 0) {
                continue;
            }
            String name = applyPlaceholders(quota.getName(), companyName);
            if (name == null || name.isBlank()) {
                name = "category-" + (result.size() + 1);
            }

            List<String> tags = quota.getTags() == null ? List.of() : quota.getTags().stream()
                    .map(tag -> applyPlaceholders(tag, companyName))
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                    .distinct()
                    .toList();

            Map<String, List<String>> facets = resolveFacets(quota.getFacets(), companyName);

            result.add(new CategoryQuota(name, limit, tags, facets));
        }
        return result;
    }

    private Map<String, List<String>> resolveFacets(Map<String, List<String>> facets, String companyName) {
        if (facets == null || facets.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> resolved = new LinkedHashMap<>();
        facets.forEach((key, values) -> {
            String resolvedKey = applyPlaceholders(key, companyName);
            if (resolvedKey == null || resolvedKey.isBlank()) {
                return;
            }
            List<String> normalizedValues = new ArrayList<>();
            if (values != null) {
                for (String value : values) {
                    String resolvedValue = applyPlaceholders(value, companyName);
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

    private Map<String, String> deriveDefaults(String type, String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return Map.of();
        }
        String normalized = slugify(companyName);
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "greenhouse" -> Map.of("slug", normalized);
            case "lever" -> Map.of("company", normalized);
            case "workday" -> Map.of(
                    "baseUrl", "https://" + normalized + ".wd1.myworkdayjobs.com",
                    "tenant", normalized,
                    "site", normalized.toUpperCase(Locale.ROOT)
            );
            default -> Map.of();
        };
    }

    private String applyPlaceholders(String value, String companyName) {
        if (value == null) {
            return null;
        }
        String trimmed = companyName == null ? "" : companyName.trim();
        String slug = slugify(trimmed);
        return value
                .replace("{{company}}", trimmed)
                .replace("{{companyLower}}", trimmed.toLowerCase(Locale.ROOT))
                .replace("{{companyUpper}}", trimmed.toUpperCase(Locale.ROOT))
                .replace("{{slug}}", slug)
                .replace("{{slugUpper}}", slug.toUpperCase(Locale.ROOT));
    }

    private String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
