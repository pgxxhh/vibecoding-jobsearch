package com.vibe.jobs.crawler.infrastructure.jpa;

import com.vibe.jobs.crawler.domain.CrawlRun;
import com.vibe.jobs.crawler.domain.CrawlRunRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCrawlRunRepository implements CrawlRunRepository {

    private final SpringDataCrawlerRunLogRepository runLogRepository;

    public JpaCrawlRunRepository(SpringDataCrawlerRunLogRepository runLogRepository) {
        this.runLogRepository = runLogRepository;
    }

    @Override
    public void save(CrawlRun run) {
        if (run == null) {
            return;
        }
        runLogRepository.save(new CrawlerRunLogEntity(
                run.id(),
                run.blueprintCode(),
                run.dataSourceCode(),
                run.company(),
                run.pageIndex(),
                run.jobCount(),
                run.durationMs(),
                run.success(),
                run.error(),
                run.startedAt(),
                run.completedAt()
        ));
    }
}
