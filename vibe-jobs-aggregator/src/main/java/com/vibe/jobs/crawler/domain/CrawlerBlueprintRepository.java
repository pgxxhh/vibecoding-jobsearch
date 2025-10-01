package com.vibe.jobs.crawler.domain;

import java.util.List;
import java.util.Optional;

public interface CrawlerBlueprintRepository {
    Optional<CrawlBlueprint> findByCode(String code);
    List<CrawlBlueprint> findAllEnabled();
}
