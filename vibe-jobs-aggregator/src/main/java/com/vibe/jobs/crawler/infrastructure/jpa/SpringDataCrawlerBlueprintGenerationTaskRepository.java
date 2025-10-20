package com.vibe.jobs.crawler.infrastructure.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataCrawlerBlueprintGenerationTaskRepository extends JpaRepository<CrawlerBlueprintGenerationTaskEntity, Long> {

    List<CrawlerBlueprintGenerationTaskEntity> findByBlueprintCodeOrderByIdDesc(String blueprintCode, Pageable pageable);

    List<CrawlerBlueprintGenerationTaskEntity> findAllByOrderByIdDesc(Pageable pageable);
}
