package com.vibe.jobs.ingestion.infrastructure.sourceclient;

import com.vibe.jobs.crawler.application.CrawlerOrchestrator;
import com.vibe.jobs.crawler.domain.CrawlContext;

import java.util.List;
import java.util.Map;

public class CrawlerSourceClient implements SourceClient {

    private final String blueprintCode;
    private final CrawlerOrchestrator orchestrator;
    private final CrawlContext context;

    public CrawlerSourceClient(String blueprintCode,
                               CrawlerOrchestrator orchestrator,
                               String dataSourceCode,
                               String company,
                               String sourceName,
                               String entryUrlOverride,
                               Map<String, String> options) {
        this.blueprintCode = blueprintCode;
        this.orchestrator = orchestrator;
        this.context = new CrawlContext(dataSourceCode, company, sourceName, entryUrlOverride, options);
    }

    @Override
    public String sourceName() {
        return context.sourceName().isBlank() ? "crawler:" + blueprintCode : context.sourceName();
    }

    @Override
    public List<FetchedJob> fetchPage(int page, int size) {
        return orchestrator.fetchJobs(blueprintCode, context, page, size);
    }
}
