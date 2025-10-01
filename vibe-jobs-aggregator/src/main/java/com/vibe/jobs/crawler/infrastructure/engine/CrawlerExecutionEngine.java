package com.vibe.jobs.crawler.infrastructure.engine;

import com.vibe.jobs.crawler.domain.CrawlPagination;
import com.vibe.jobs.crawler.domain.CrawlSession;

public interface CrawlerExecutionEngine {
    CrawlPageSnapshot fetch(CrawlSession session, CrawlPagination pagination) throws Exception;
}
