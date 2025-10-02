package com.vibe.jobs.crawler.application;

import com.vibe.jobs.crawler.application.dto.CrawlPageRequest;
import com.vibe.jobs.crawler.application.dto.CrawlPageResult;
import com.vibe.jobs.crawler.domain.*;
import com.vibe.jobs.crawler.infrastructure.engine.CrawlPageSnapshot;
import com.vibe.jobs.crawler.infrastructure.engine.HybridCrawlerExecutionEngine;
import com.vibe.jobs.crawler.infrastructure.parser.CrawlerParserEngine;
import com.vibe.jobs.sources.FetchedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CrawlerOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CrawlerOrchestrator.class);

    private final CrawlerBlueprintRepository blueprintRepository;
    private final CrawlRunRepository runRepository;
    private final HybridCrawlerExecutionEngine executionEngine;
    private final CrawlerParserEngine parserEngine;
    private final Clock clock;

    public CrawlerOrchestrator(CrawlerBlueprintRepository blueprintRepository,
                               CrawlRunRepository runRepository,
                               HybridCrawlerExecutionEngine executionEngine,
                               CrawlerParserEngine parserEngine,
                               java.util.Optional<Clock> clock) {
        this.blueprintRepository = blueprintRepository;
        this.runRepository = runRepository;
        this.executionEngine = executionEngine;
        this.parserEngine = parserEngine;
        this.clock = clock.orElse(Clock.systemUTC());
    }

    public CrawlPageResult execute(CrawlPageRequest request) {
        Objects.requireNonNull(request, "request");
        Instant started = clock.instant();
        CrawlBlueprint blueprint = blueprintRepository.findByCode(request.blueprintCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown crawler blueprint: " + request.blueprintCode()));
        if (!blueprint.enabled() || !blueprint.isConfigured()) {
            log.info("Crawler blueprint {} disabled or misconfigured", blueprint.code());
            return new CrawlPageResult(List.of(), new CrawlMetrics(0, 0, request.pagination().page(), true, "disabled"));
        }

        CrawlSession session = new CrawlSession(blueprint, request.context());
        List<CrawlResult> results = new ArrayList<>();
        boolean success = false;
        String error = "";
        try {
            CrawlPageSnapshot snapshot = executionEngine.fetch(session, request.pagination());
            results = parserEngine.parse(session, snapshot);
            success = true;
        } catch (Exception ex) {
            error = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            log.warn("Crawler execution failed for blueprint {} page {}: {}", blueprint.code(), request.pagination().page(), error);
            log.debug("Crawler execution error", ex);
        }
        Instant completed = clock.instant();
        long duration = Math.max(0, completed.toEpochMilli() - started.toEpochMilli());
        CrawlMetrics metrics = new CrawlMetrics(duration, results.size(), request.pagination().page(), success, error);
        recordRun(session, request.pagination(), metrics);
        return new CrawlPageResult(results, metrics);
    }

    public List<FetchedJob> fetchJobs(String blueprintCode, CrawlContext context, int page, int size) {
        CrawlPageResult result = execute(new CrawlPageRequest(blueprintCode, context, new CrawlPagination(page, size)));
        return result.results().stream().map(CrawlResult::job).toList();
    }

    private void recordRun(CrawlSession session, CrawlPagination pagination, CrawlMetrics metrics) {
        try {
            CrawlRun run = new CrawlRun(
                    UUID.randomUUID().toString(),
                    session.blueprint().code(),
                    session.context().dataSourceCode(),
                    session.context().company(),
                    pagination.page(),
                    metrics.jobCount(),
                    metrics.durationMs(),
                    metrics.success(),
                    metrics.error(),
                    session.startedAt(),
                    clock.instant()
            );
            runRepository.save(run);
        } catch (Exception ex) {
            log.debug("Failed to persist crawler run log: {}", ex.getMessage());
        }
    }
}
