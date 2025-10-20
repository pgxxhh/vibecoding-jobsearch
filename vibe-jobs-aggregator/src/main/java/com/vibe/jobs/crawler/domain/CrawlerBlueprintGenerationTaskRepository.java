package com.vibe.jobs.crawler.domain;

import java.util.List;
import java.util.Optional;

public interface CrawlerBlueprintGenerationTaskRepository {

    CrawlerBlueprintGenerationTask save(CrawlerBlueprintGenerationTask task);

    Optional<CrawlerBlueprintGenerationTask> findById(Long id);

    List<CrawlerBlueprintGenerationTask> findRecentForBlueprint(String blueprintCode, int limit);

    List<CrawlerBlueprintGenerationTask> findRecent(int page, int size);
}
