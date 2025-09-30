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
import java.util.Objects;
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

            // 检查是否为单实例API客户端（如Apple、Microsoft等官方API）
            if (isSingleInstanceApiClient(type)) {
                // 对于单实例客户端，不需要遍历公司列表，只创建一个实例
                String cacheKey = source.key();
                PlaceholderContext context = buildPlaceholderContext("");
                Map<String, String> mergedOptions = source.getOptions();
                List<CategoryQuota> categories = resolveCategories(source, context);
                try {
                    SourceClient client = clientCache.computeIfAbsent(cacheKey, key -> factory.create(type, mergedOptions));
                    resolved.add(new ConfiguredSource(source, type.toUpperCase(), client, categories));
                } catch (Exception ex) {
                    log.warn("Failed to initialize single-instance source {}: {}", source.displayName(), ex.getMessage());
                    log.debug("Source initialization error", ex);
                }
                continue;
            }

            // 对于公司特定的客户端，遍历公司列表
            for (String companyName : properties.getCompanies()) {
                if (companyName == null || companyName.isBlank()) {
                    continue;
                }
                String trimmedCompany = companyName.trim();
                PlaceholderContext context = buildPlaceholderContext(trimmedCompany);
                Map<String, String> mergedOptions = mergeOptions(type, source, trimmedCompany, context);
                if (mergedOptions.isEmpty()) {
                    continue;
                }
                String cacheKey = source.key() + "::" + trimmedCompany;
                List<CategoryQuota> categories = resolveCategories(source, context);
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
    
    /**
     * 判断是否为单实例API客户端
     */
    private boolean isSingleInstanceApiClient(String type) {
        if (type == null) return false;
        String normalized = type.toLowerCase(Locale.ROOT);
        return normalized.equals("apple-api") || 
               normalized.equals("microsoft-api") || 
               normalized.equals("amazon-api") ||
               normalized.endsWith("-api");  // 通用规则：以-api结尾的都视为单实例客户端
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
                                             String companyName,
                                             PlaceholderContext context) {
        IngestionProperties.SourceOverride override = properties.getSourceOverride(companyName, type);

        if (override != null && !override.isEnabled()) {
            return Map.of();
        }
        if (source.isRequireOverride() && override == null) {
            return Map.of();
        }

        Map<String, String> overrideOptions = override == null ? Map.of() : override.optionsCopy();

        Map<String, String> result = new HashMap<>(source.getOptions());
        result.replaceAll((k, v) -> applyPlaceholders(v, context));

        Map<String, String> derived = deriveDefaults(type, context);
        derived.forEach(result::putIfAbsent);

        overrideOptions.forEach(result::put);

        if (!context.company().isBlank()) {
            result.putIfAbsent("company", context.company());
        }

        result.replaceAll((k, v) -> applyPlaceholders(v, context));

        result.values().removeIf(value -> value == null || value.isBlank());

        return result;
    }

    private List<CategoryQuota> resolveCategories(IngestionProperties.Source source, PlaceholderContext context) {
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
            String name = applyPlaceholders(quota.getName(), context);
            if (name == null || name.isBlank()) {
                name = "category-" + (result.size() + 1);
            }

            List<String> tags = quota.getTags() == null ? List.of() : quota.getTags().stream()
                    .map(tag -> applyPlaceholders(tag, context))
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                    .distinct()
                    .toList();

            Map<String, List<String>> facets = resolveFacets(quota.getFacets(), context);

            result.add(new CategoryQuota(name, limit, tags, facets));
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

    private Map<String, String> deriveDefaults(String type, PlaceholderContext context) {
        if (context.company().isBlank()) {
            return Map.of();
        }
        String normalized = context.slug();
        if (normalized.isBlank()) {
            return Map.of();
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "greenhouse" -> Map.of("slug", normalized);
            case "lever" -> Map.of("company", normalized);
            case "workday" -> Map.of(
                    "baseUrl", "https://" + normalized + ".wd1.myworkdayjobs.com",
                    "tenant", normalized,
                    "site", context.slugUpper()
            );
            case "ashby" -> Map.of(
                    "company", context.company(),
                    "baseUrl", "https://jobs.ashbyhq.com/" + normalized
            );
            default -> Map.of();
        };
    }

    private String applyPlaceholders(String value, PlaceholderContext context) {
        if (value == null) {
            return null;
        }
        String result = value
                .replace("{{company}}", context.company())
                .replace("{{companyLower}}", context.companyLower())
                .replace("{{companyUpper}}", context.companyUpper())
                .replace("{{slug}}", context.slug())
                .replace("{{slugUpper}}", context.slugUpper());
        for (Map.Entry<String, String> entry : context.custom().entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private PlaceholderContext buildPlaceholderContext(String companyName) {
        String trimmed = Objects.requireNonNullElse(companyName, "").trim();
        Map<String, String> overrides = new LinkedHashMap<>(properties.getPlaceholderOverrides(trimmed));

        String company = overrides.containsKey("company")
                ? overrides.remove("company")
                : trimmed;
        company = company == null ? "" : company.trim();

        String slug = overrides.containsKey("slug")
                ? overrides.remove("slug")
                : slugify(company.isBlank() ? trimmed : company);
        slug = slug == null ? "" : slug.trim();

        String slugUpper = overrides.containsKey("slugUpper")
                ? overrides.remove("slugUpper")
                : slug.toUpperCase(Locale.ROOT);
        slugUpper = slugUpper == null ? "" : slugUpper.trim();

        String companyLower = overrides.containsKey("companyLower")
                ? overrides.remove("companyLower")
                : company.toLowerCase(Locale.ROOT);
        companyLower = companyLower == null ? "" : companyLower.trim();

        String companyUpper = overrides.containsKey("companyUpper")
                ? overrides.remove("companyUpper")
                : company.toUpperCase(Locale.ROOT);
        companyUpper = companyUpper == null ? "" : companyUpper.trim();

        return new PlaceholderContext(
                company,
                companyLower,
                companyUpper,
                slug,
                slugUpper,
                overrides
        );
    }

    private record PlaceholderContext(String company,
                                      String companyLower,
                                      String companyUpper,
                                      String slug,
                                      String slugUpper,
                                      Map<String, String> custom) { }

    private String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
