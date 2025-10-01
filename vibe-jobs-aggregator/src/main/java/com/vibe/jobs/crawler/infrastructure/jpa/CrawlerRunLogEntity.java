package com.vibe.jobs.crawler.infrastructure.jpa;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "crawler_run_log")
public class CrawlerRunLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", length = 128, unique = true)
    private String runId;

    @Column(name = "blueprint_code", length = 128)
    private String blueprintCode;

    @Column(name = "data_source_code", length = 128)
    private String dataSourceCode;

    @Column(length = 128)
    private String company;

    @Column(name = "page_index")
    private int pageIndex;

    @Column(name = "job_count")
    private int jobCount;

    @Column(name = "duration_ms")
    private long durationMs;

    @Column(name = "success")
    private boolean success;

    @Column(name = "error", length = 1024)
    private String error;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public CrawlerRunLogEntity() {
    }

    public CrawlerRunLogEntity(String runId,
                               String blueprintCode,
                               String dataSourceCode,
                               String company,
                               int pageIndex,
                               int jobCount,
                               long durationMs,
                               boolean success,
                               String error,
                               Instant startedAt,
                               Instant completedAt) {
        this.runId = runId;
        this.blueprintCode = blueprintCode;
        this.dataSourceCode = dataSourceCode;
        this.company = company;
        this.pageIndex = pageIndex;
        this.jobCount = jobCount;
        this.durationMs = durationMs;
        this.success = success;
        this.error = error;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }
}
