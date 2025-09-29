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
    private final Map<String, ConfiguredSource> cache = new ConcurrentHashMap<>();

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
                try {
                    ConfiguredSource configured = cache.computeIfAbsent(cacheKey, key -> {
                        SourceClient client = factory.create(type, mergedOptions);
                        return new ConfiguredSource(source, trimmedCompany, client);
                    });
                    resolved.add(configured);
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
                                   SourceClient client) { }

    private Map<String, String> mergeOptions(String type,
                                             IngestionProperties.Source source,
                                             String companyName) {
        PlaceholderContext context = buildPlaceholderContext(companyName);
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
