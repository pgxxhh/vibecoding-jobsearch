package com.vibe.jobs.crawler.infrastructure.parser;

import com.vibe.jobs.crawler.domain.CrawlSession;
import com.vibe.jobs.crawler.infrastructure.engine.CrawlPageSnapshot;

import java.util.List;

import com.vibe.jobs.crawler.domain.CrawlResult;

public interface CrawlerParserEngine {
    List<CrawlResult> parse(CrawlSession session, CrawlPageSnapshot snapshot);
}
