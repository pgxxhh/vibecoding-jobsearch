package com.vibe.jobs.crawler.domain;

import java.util.Optional;

public interface CrawlerParserTemplateRepository {
    Optional<ParserProfile> findByCode(String code);
}
