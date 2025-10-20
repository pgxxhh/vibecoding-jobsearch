package com.vibe.jobs.crawler.infrastructure.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataCrawlerBlueprintJpaRepository extends JpaRepository<CrawlerBlueprintEntity, String> {
    List<CrawlerBlueprintEntity> findAllByEnabledTrue();

    List<CrawlerBlueprintEntity> findByStatusInOrderByUpdatedAtDesc(List<String> statuses, Pageable pageable);

    List<CrawlerBlueprintEntity> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
