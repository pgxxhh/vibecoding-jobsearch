package com.vibe.jobs.crawler.infrastructure.engine;

import com.vibe.jobs.crawler.domain.CrawlBlueprint;
import com.vibe.jobs.crawler.domain.CrawlPagination;
import com.vibe.jobs.crawler.domain.CrawlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hybrid crawler execution engine that automatically selects between HTTP and JavaScript engines
 * based on the blueprint configuration. This provides seamless support for both static and
 * JavaScript-rendered pages.
 */
@Component
public class HybridCrawlerExecutionEngine implements CrawlerExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(HybridCrawlerExecutionEngine.class);
    
    private final HttpCrawlerExecutionEngine httpEngine;
    private final JsCrawlerExecutionEngine jsEngine;

    public HybridCrawlerExecutionEngine(HttpCrawlerExecutionEngine httpEngine, JsCrawlerExecutionEngine jsEngine) {
        this.httpEngine = httpEngine;
        this.jsEngine = jsEngine;
    }

    @Override
    public CrawlPageSnapshot fetch(CrawlSession session, CrawlPagination pagination) throws Exception {
        CrawlBlueprint blueprint = session.blueprint();
        
        // Check if JavaScript rendering is enabled for this blueprint
        boolean jsEnabled = blueprint.metadata("jsEnabled")
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
        
        if (jsEnabled) {
            log.debug("Using JavaScript engine for blueprint {}", blueprint.code());
            return jsEngine.fetch(session, pagination);
        } else {
            log.debug("Using HTTP engine for blueprint {}", blueprint.code());
            return httpEngine.fetch(session, pagination);
        }
    }
}