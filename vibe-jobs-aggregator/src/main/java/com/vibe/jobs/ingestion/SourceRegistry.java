package com.vibe.jobs.ingestion;

import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.sources.SourceClient;
import com.vibe.jobs.sources.SourceClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
