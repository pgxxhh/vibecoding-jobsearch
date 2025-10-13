package com.vibe.jobs.crawler.infrastructure.engine;

import com.vibe.jobs.crawler.domain.CrawlBlueprint;
import com.vibe.jobs.crawler.domain.CrawlPagination;
import com.vibe.jobs.crawler.domain.CrawlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HybridCrawlerExecutionEngine implements CrawlerExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(HybridCrawlerExecutionEngine.class);

    private final HttpCrawlerExecutionEngine httpEngine;
    private final BrowserCrawlerExecutionEngine browserEngine;

    public HybridCrawlerExecutionEngine(HttpCrawlerExecutionEngine httpEngine,
                                        BrowserCrawlerExecutionEngine browserEngine) {
        this.httpEngine = httpEngine;
        this.browserEngine = browserEngine;
    }

    @Override
    public CrawlPageSnapshot fetch(CrawlSession session, CrawlPagination pagination) throws Exception {
        CrawlBlueprint blueprint = session.blueprint();
        if (browserEngine != null && browserEngine.supports(blueprint)) {
            try {
                log.info("Using browser engine for blueprint {}", blueprint.code());
                return browserEngine.fetch(session, pagination);
            } catch (Exception ex) {
                log.warn("Browser engine failed for blueprint {}, falling back to HTTP: {}", blueprint.code(), ex.getMessage());
            }
        }
        log.info("Using HTTP engine for blueprint {}", blueprint.code());
        return httpEngine.fetch(session, pagination);
    }
}