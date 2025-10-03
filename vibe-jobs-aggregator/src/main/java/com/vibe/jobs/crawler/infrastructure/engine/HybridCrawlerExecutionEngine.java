package com.vibe.jobs.crawler.infrastructure.engine;

import com.vibe.jobs.crawler.domain.CrawlBlueprint;
import com.vibe.jobs.crawler.domain.CrawlPagination;
import com.vibe.jobs.crawler.domain.CrawlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hybrid crawler execution engine that uses HTTP-based crawling for all sites.
 * JavaScript crawling has been removed to simplify deployment and reduce resource usage.
 */
@Component
public class HybridCrawlerExecutionEngine implements CrawlerExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(HybridCrawlerExecutionEngine.class);
    
    private final HttpCrawlerExecutionEngine httpEngine;

    public HybridCrawlerExecutionEngine(HttpCrawlerExecutionEngine httpEngine) {
        this.httpEngine = httpEngine;
    }

    @Override
    public CrawlPageSnapshot fetch(CrawlSession session, CrawlPagination pagination) throws Exception {
        CrawlBlueprint blueprint = session.blueprint();
        
        // Always use HTTP engine (JavaScript crawling removed)
        log.debug("Using HTTP engine for blueprint {}", blueprint.code());
        return httpEngine.fetch(session, pagination);
    }
}