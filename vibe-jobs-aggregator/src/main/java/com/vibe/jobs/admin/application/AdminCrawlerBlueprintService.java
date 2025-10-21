package com.vibe.jobs.admin.application;

import com.vibe.jobs.admin.application.AdminChangeLogService;
import com.vibe.jobs.crawler.application.generation.CrawlerBlueprintGenerationManager;
import com.vibe.jobs.crawler.application.generation.CrawlerBlueprintGenerationManager.GenerationCommand;
import com.vibe.jobs.crawler.application.generation.CrawlerBlueprintGenerationManager.GenerationLaunchResult;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintDraft;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintGenerationTask;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintStatus;
import com.vibe.jobs.datasource.application.DataSourceCommandService;
import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminCrawlerBlueprintService {

    private final CrawlerBlueprintGenerationManager generationManager;
    private final DataSourceCommandService dataSourceCommandService;
    private final JobDataSourceRepository jobDataSourceRepository;
    private final AdminChangeLogService changeLogService;
    public AdminCrawlerBlueprintService(CrawlerBlueprintGenerationManager generationManager,
                                        DataSourceCommandService dataSourceCommandService,
                                        JobDataSourceRepository jobDataSourceRepository,
                                        AdminChangeLogService changeLogService) {
        this.generationManager = generationManager;
        this.dataSourceCommandService = dataSourceCommandService;
        this.jobDataSourceRepository = jobDataSourceRepository;
        this.changeLogService = changeLogService;
    }

    public GenerationLaunchResult launchGeneration(String requestedCode,
                                                   String name,
                                                   String entryUrl,
                                                   String searchKeywords,
                                                   List<String> excludeSelectors,
                                                   String notes,
                                                   String operatorEmail) {
        GenerationCommand command = new GenerationCommand(
                requestedCode,
                name,
                entryUrl,
                searchKeywords,
                excludeSelectors,
                notes,
                operatorEmail
        );
        return generationManager.launchGeneration(command);
    }

    public GenerationLaunchResult rerunGeneration(String blueprintCode,
                                                  String name,
                                                  String entryUrl,
                                                  String searchKeywords,
                                                  List<String> excludeSelectors,
                                                  String notes,
                                                  String operatorEmail) {
        return launchGeneration(blueprintCode, name, entryUrl, searchKeywords, excludeSelectors, notes, operatorEmail);
    }

    public Optional<CrawlerBlueprintDraft> findDraft(String code) {
        return generationManager.findDraft(code);
    }

    public List<CrawlerBlueprintDraft> listDrafts(List<CrawlerBlueprintStatus> statuses, int page, int size) {
        return generationManager.listDrafts(statuses, page, size);
    }

    public List<CrawlerBlueprintGenerationTask> listTasks(String code, int limit) {
        return generationManager.listTasks(code, limit);
    }

    public Optional<CrawlerBlueprintGenerationTask> findTask(Long id) {
        return generationManager.findTask(id);
    }

    @Transactional
    public ActivationResult activateBlueprint(String code,
                                               String desiredDataSourceCode,
                                               boolean enable,
                                               String operatorEmail) {
        CrawlerBlueprintDraft activated = generationManager.activate(code, operatorEmail);
        JobDataSource dataSource = upsertDataSource(code,
                desiredDataSourceCode == null || desiredDataSourceCode.isBlank() ? code : desiredDataSourceCode.trim(),
                enable);

        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("blueprint", Map.of("code", code, "status", activated.status().name()));
        diff.put("dataSource", Map.of("code", dataSource.getCode()));
        changeLogService.record(operatorEmail,
                "ACTIVATE",
                "CRAWLER_BLUEPRINT",
                code,
                diff);

        return new ActivationResult(activated, dataSource);
    }

    private JobDataSource upsertDataSource(String blueprintCode,
                                           String dataSourceCode,
                                           boolean enable) {
        Map<String, String> baseOptions = new LinkedHashMap<>();
        jobDataSourceRepository.findByCode(dataSourceCode).ifPresent(existing -> baseOptions.putAll(existing.getBaseOptions()));
        baseOptions.put("blueprintCode", blueprintCode);
        baseOptions.put("crawlerBlueprintCode", blueprintCode);

        JobDataSource existing = jobDataSourceRepository.findByCode(dataSourceCode).orElse(null);
        JobDataSource source = existing == null
                ? new JobDataSource(
                null,
                dataSourceCode,
                "crawler",
                enable,
                false,
                false,
                JobDataSource.Flow.UNLIMITED,
                baseOptions,
                List.of(),
                List.of(),
                blueprintCode,
                true)
                : new JobDataSource(
                existing.getId(),
                existing.getCode(),
                existing.getType(),
                enable,
                existing.isRunOnStartup(),
                existing.isRequireOverride(),
                existing.getFlow(),
                baseOptions,
                existing.getCategories(),
                existing.getCompanies(),
                blueprintCode,
                true);
        return dataSourceCommandService.save(source.normalized());
    }

    public record ActivationResult(CrawlerBlueprintDraft draft,
                                   JobDataSource dataSource) {
    }
}
