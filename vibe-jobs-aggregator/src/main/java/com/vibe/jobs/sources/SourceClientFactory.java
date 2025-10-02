package com.vibe.jobs.sources;

import com.vibe.jobs.crawler.application.CrawlerOrchestrator;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class SourceClientFactory {

    private final CrawlerOrchestrator crawlerOrchestrator;

    public SourceClientFactory(CrawlerOrchestrator crawlerOrchestrator) {
        this.crawlerOrchestrator = crawlerOrchestrator;
    }

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
            case "smartrecruiters" -> new SmartRecruitersSourceClient(
                    require(opts, "company"),
                    opts.get("baseUrl")
            );
            case "recruitee" -> new RecruiteeSourceClient(
                    require(opts, "company"),
                    opts.get("baseUrl")
            );
            case "workable" -> new WorkableSourceClient(
                    require(opts, "company"),
                    opts.get("baseUrl")
            );
            case "crawler" -> new CrawlerSourceClient(
                    resolveBlueprint(opts),
                    crawlerOrchestrator,
                    opts.getOrDefault("__sourceCode", ""),
                    opts.getOrDefault("__company", ""),
                    opts.getOrDefault("sourceName", opts.getOrDefault("__sourceName", "")),
                    opts.getOrDefault("entryUrl", ""),
                    opts
            );
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

    private String resolveBlueprint(Map<String, String> options) {
        String blueprint = options.getOrDefault("blueprintCode", options.getOrDefault("crawlerBlueprintCode", ""));
        if (blueprint == null || blueprint.isBlank()) {
            throw new IllegalArgumentException("Missing required option 'blueprintCode'");
        }
        return blueprint;
    }
}
