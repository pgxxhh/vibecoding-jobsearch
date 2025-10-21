package com.vibe.jobs.crawler.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CrawlerBlueprintGenerationTask {

    private final Long id;
    private final String blueprintCode;
    private final Map<String, Object> inputPayload;
    private final Status status;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final String errorMessage;
    private final Map<String, Object> browserSnapshot;
    private final List<Map<String, Object>> sampleData;
    private final Instant createTime;
    private final Instant updateTime;

    public CrawlerBlueprintGenerationTask(Long id,
                                          String blueprintCode,
                                          Map<String, Object> inputPayload,
                                          Status status,
                                          Instant startedAt,
                                          Instant finishedAt,
                                          String errorMessage,
                                          Map<String, Object> browserSnapshot,
                                          List<Map<String, Object>> sampleData,
                                          Instant createTime,
                                          Instant updateTime) {
        this.id = id;
        this.blueprintCode = blueprintCode == null ? "" : blueprintCode.trim();
        this.inputPayload = inputPayload == null ? Map.of() : Map.copyOf(inputPayload);
        this.status = status == null ? Status.PENDING : status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
        this.browserSnapshot = browserSnapshot == null ? Map.of() : Map.copyOf(browserSnapshot);
        this.sampleData = sampleData == null ? List.of() : List.copyOf(sampleData);
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public static CrawlerBlueprintGenerationTask create(String blueprintCode,
                                                        Map<String, Object> inputPayload) {
        return new CrawlerBlueprintGenerationTask(null,
                blueprintCode,
                inputPayload,
                Status.PENDING,
                null,
                null,
                null,
                Map.of(),
                List.of(),
                null,
                null);
    }

    public Long id() {
        return id;
    }

    public String blueprintCode() {
        return blueprintCode;
    }

    public Map<String, Object> inputPayload() {
        return inputPayload;
    }

    public Status status() {
        return status;
    }

    public Optional<Instant> startedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<Instant> finishedAt() {
        return Optional.ofNullable(finishedAt);
    }

    public String errorMessage() {
        return errorMessage;
    }

    public Map<String, Object> browserSnapshot() {
        return browserSnapshot;
    }

    public List<Map<String, Object>> sampleData() {
        return sampleData;
    }

    public Optional<Instant> createTime() {
        return Optional.ofNullable(createTime);
    }

    public Optional<Instant> updateTime() {
        return Optional.ofNullable(updateTime);
    }

    public CrawlerBlueprintGenerationTask markRunning(Instant startedAt) {
        return new CrawlerBlueprintGenerationTask(
                id,
                blueprintCode,
                inputPayload,
                Status.RUNNING,
                startedAt,
                null,
                null,
                browserSnapshot,
                sampleData,
                createTime,
                startedAt
        );
    }

    public CrawlerBlueprintGenerationTask markSucceeded(Instant finishedAt,
                                                        Map<String, Object> snapshot,
                                                        List<Map<String, Object>> samples) {
        return new CrawlerBlueprintGenerationTask(
                id,
                blueprintCode,
                inputPayload,
                Status.SUCCEEDED,
                startedAt,
                finishedAt,
                null,
                snapshot == null ? Map.of() : Map.copyOf(snapshot),
                samples == null ? List.of() : List.copyOf(samples),
                createTime,
                finishedAt
        );
    }

    public CrawlerBlueprintGenerationTask markFailed(Instant finishedAt,
                                                     String error,
                                                     Map<String, Object> snapshot) {
        return new CrawlerBlueprintGenerationTask(
                id,
                blueprintCode,
                inputPayload,
                Status.FAILED,
                startedAt,
                finishedAt,
                error,
                snapshot == null ? Map.of() : Map.copyOf(snapshot),
                sampleData,
                createTime,
                finishedAt
        );
    }

    public enum Status {
        PENDING,
        RUNNING,
        SUCCEEDED,
        FAILED
    }
}
