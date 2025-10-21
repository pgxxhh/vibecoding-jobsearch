package com.vibe.jobs.crawler.infrastructure.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintGenerationTask;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintGenerationTaskRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JpaCrawlerBlueprintGenerationTaskRepository implements CrawlerBlueprintGenerationTaskRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> LIST_TYPE = new TypeReference<>() {};

    private final SpringDataCrawlerBlueprintGenerationTaskRepository repository;
    private final ObjectMapper objectMapper;

    public JpaCrawlerBlueprintGenerationTaskRepository(SpringDataCrawlerBlueprintGenerationTaskRepository repository,
                                                       ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CrawlerBlueprintGenerationTask save(CrawlerBlueprintGenerationTask task) {
        CrawlerBlueprintGenerationTaskEntity entity = task.id() == null
                ? new CrawlerBlueprintGenerationTaskEntity()
                : repository.findById(task.id()).orElse(new CrawlerBlueprintGenerationTaskEntity());
        entity.setId(task.id());
        entity.setBlueprintCode(task.blueprintCode());
        entity.setInputPayload(writeJson(task.inputPayload()));
        entity.setStatus(task.status().name());
        entity.setStartedAt(task.startedAt().orElse(null));
        entity.setFinishedAt(task.finishedAt().orElse(null));
        entity.setErrorMessage(task.errorMessage());
        entity.setBrowserSessionSnapshot(writeJson(task.browserSnapshot()));
        entity.setSampleData(writeJson(task.sampleData()));
        entity.setDeleted(false);
        return map(repository.save(entity));
    }

    @Override
    public Optional<CrawlerBlueprintGenerationTask> findById(Long id) {
        return repository.findById(id).map(this::map);
    }

    @Override
    public List<CrawlerBlueprintGenerationTask> findRecentForBlueprint(String blueprintCode, int limit) {
        return repository.findByBlueprintCodeOrderByIdDesc(blueprintCode,
                        PageRequest.of(0, Math.max(1, limit))).stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<CrawlerBlueprintGenerationTask> findRecent(int page, int size) {
        return repository.findAllByOrderByIdDesc(PageRequest.of(Math.max(0, page), Math.max(1, size))).stream()
                .map(this::map)
                .toList();
    }

    private CrawlerBlueprintGenerationTask map(CrawlerBlueprintGenerationTaskEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CrawlerBlueprintGenerationTask(
                entity.getId(),
                entity.getBlueprintCode(),
                readMap(entity.getInputPayload()),
                CrawlerBlueprintGenerationTask.Status.valueOf(entity.getStatus()),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getErrorMessage(),
                readMap(entity.getBrowserSessionSnapshot()),
                readList(entity.getSampleData()),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (IOException e) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_TYPE);
        } catch (IOException e) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            return null;
        }
    }
}
