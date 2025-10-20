package com.vibe.jobs.crawler.domain;

import java.util.List;
import java.util.Optional;

public interface CrawlerBlueprintDraftRepository {

    CrawlerBlueprintDraft save(CrawlerBlueprintDraft draft);

    Optional<CrawlerBlueprintDraft> findByCode(String code);

    List<CrawlerBlueprintDraft> findByStatus(List<CrawlerBlueprintStatus> statuses, int page, int size);

    List<CrawlerBlueprintDraft> findRecent(int page, int size);
}
