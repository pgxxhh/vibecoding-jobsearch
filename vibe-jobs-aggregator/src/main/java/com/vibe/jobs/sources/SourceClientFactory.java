package com.vibe.jobs.sources;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class SourceClientFactory {

    public SourceClient create(String type, Map<String, String> options) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Source type must be provided");
        }

        String normalized = type.toLowerCase(Locale.ROOT);
        Map<String, String> opts = options == null ? Map.of() : options;

        return switch (normalized) {
            case "greenhouse" -> new GreenhouseSourceClient(require(opts, "slug"));
            case "lever" -> new LeverSourceClient(require(opts, "company"));
            case "workday" -> new WorkdaySourceClient(
                    opts.getOrDefault("company", require(opts, "tenant")),
                    require(opts, "baseUrl"),
                    require(opts, "tenant"),
                    require(opts, "site")
            );
            case "standard" -> new StandardCareersApiSourceClient(
                    require(opts, "company"),
                    require(opts, "apiUrl"),
                    opts.getOrDefault("jobsPath", "/jobs")
            );
            case "ashby" -> new AshbySourceClient(
                    require(opts, "company"),
                    require(opts, "baseUrl")
            );
            case "generic", "moka", "beisen", "successfactors", "taleo", "icims", "smartrecruiters", "avature" ->
                new GenericAtsSourceClient(
                    require(opts, "company"),
                    require(opts, "baseUrl"),
                    opts.get("searchPath"),
                    opts.entrySet().stream()
                        .filter(e -> e.getKey().startsWith("param_"))
                        .collect(java.util.stream.Collectors.toMap(
                            e -> e.getKey().substring(6),
                            Map.Entry::getValue
                        )),
                    opts.entrySet().stream()
                        .filter(e -> e.getKey().startsWith("payload_"))
                        .collect(java.util.stream.Collectors.toMap(
                            e -> e.getKey().substring(8),
                            Map.Entry::getValue
                        )),
                    normalized
                );
            case "amazon-api" -> new AmazonJobsClient();
            default -> throw new IllegalArgumentException("Unsupported source type: " + type);
        };
    }

    private String require(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option '" + key + "'");
        }
        return value;
    }
}
