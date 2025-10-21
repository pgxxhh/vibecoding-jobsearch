package com.vibe.jobs.crawler.application.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintDraft;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintDraftRepository;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintGenerationTask;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintGenerationTaskRepository;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintStatus;
import com.vibe.jobs.crawler.infrastructure.engine.BrowserSessionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
public class CrawlerBlueprintGenerationManager {

    private static final Logger log = LoggerFactory.getLogger(CrawlerBlueprintGenerationManager.class);

    private static final String KEY_ENTRY_URL = "entryUrl";
    private static final String KEY_SEARCH_KEYWORDS = "searchKeywords";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_EXCLUDES = "excludeSelectors";
    private static final String KEY_NAME = "name";
    private static final String KEY_OPERATOR = "operator";

    private final CrawlerBlueprintDraftRepository draftRepository;
    private final CrawlerBlueprintGenerationTaskRepository taskRepository;
    private final BrowserSessionManager browserSessionManager;
    private final CrawlerBlueprintAutoParser autoParser;
    private final CrawlerBlueprintValidator validator;
    private final CrawlerBlueprintConfigFactory configFactory;
    private final ObjectMapper objectMapper;
    private final Executor generationExecutor;
    private final Clock clock;

    public CrawlerBlueprintGenerationManager(CrawlerBlueprintDraftRepository draftRepository,
                                             CrawlerBlueprintGenerationTaskRepository taskRepository,
                                             BrowserSessionManager browserSessionManager,
                                             CrawlerBlueprintAutoParser autoParser,
                                             CrawlerBlueprintValidator validator,
                                             CrawlerBlueprintConfigFactory configFactory,
                                             ObjectMapper objectMapper,
                                             @Qualifier("crawlerBlueprintGenerationExecutor") Executor generationExecutor,
                                             Optional<Clock> clock) {
        this.draftRepository = draftRepository;
        this.taskRepository = taskRepository;
        this.browserSessionManager = browserSessionManager;
        this.autoParser = autoParser;
        this.validator = validator;
        this.configFactory = configFactory;
        this.objectMapper = objectMapper;
        this.generationExecutor = generationExecutor;
        this.clock = clock.orElse(Clock.systemUTC());
    }

    public GenerationLaunchResult launchGeneration(GenerationCommand command) {
        Objects.requireNonNull(command, "command");
        String code = determineCode(command);
        Instant now = clock.instant();

        CrawlerBlueprintDraft draft = draftRepository.findByCode(code)
                .map(existing -> existing.refreshForGeneration(command.entryUrl(), command.name(), command.operatorEmail()))
                .orElseGet(() -> new CrawlerBlueprintDraft(
                        code,
                        command.name() == null || command.name().isBlank() ? code : command.name().trim(),
                        command.entryUrl(),
                        1,
                        false,
                        "",
                        "",
                        "",
                        "",
                        CrawlerBlueprintStatus.DRAFT,
                        true,
                        command.operatorEmail(),
                        null,
                        now,
                        now
                ));
        draft = draftRepository.save(draft);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(KEY_ENTRY_URL, command.entryUrl());
        payload.put(KEY_SEARCH_KEYWORDS, command.searchKeywords() == null ? "" : command.searchKeywords());
        payload.put(KEY_NOTES, command.notes() == null ? "" : command.notes());
        payload.put(KEY_EXCLUDES, command.excludeSelectors() == null
                ? List.of()
                : List.copyOf(command.excludeSelectors()));
        payload.put(KEY_NAME, command.name() == null ? "" : command.name());
        payload.put(KEY_OPERATOR, command.operatorEmail() == null ? "" : command.operatorEmail());

        CrawlerBlueprintGenerationTask task = CrawlerBlueprintGenerationTask.create(code, payload);
        CrawlerBlueprintGenerationTask persistedTask = taskRepository.save(task);

        generationExecutor.execute(() -> runGenerationTask(persistedTask.id()));

        return new GenerationLaunchResult(draft, persistedTask);
    }

    public Optional<CrawlerBlueprintDraft> findDraft(String code) {
        return draftRepository.findByCode(code);
    }

    public List<CrawlerBlueprintDraft> listDrafts(List<CrawlerBlueprintStatus> statuses, int page, int size) {
        if (statuses == null || statuses.isEmpty()) {
            return draftRepository.findRecent(page, size);
        }
        return draftRepository.findByStatus(statuses, page, size);
    }

    public List<CrawlerBlueprintGenerationTask> listTasks(String code, int limit) {
        return taskRepository.findRecentForBlueprint(code, limit);
    }

    public Optional<CrawlerBlueprintGenerationTask> findTask(Long id) {
        return taskRepository.findById(id);
    }

    public CrawlerBlueprintDraft activate(String code, String operatorEmail) {
        CrawlerBlueprintDraft draft = draftRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown blueprint draft: " + code));
        if (draft.draftConfigJson().isBlank()) {
            throw new IllegalStateException("Draft has no generated configuration");
        }
        Instant now = clock.instant();
        CrawlerBlueprintDraft activated = draft.activate(draft.draftConfigJson(), now, operatorEmail);
        return draftRepository.save(activated);
    }

    private void runGenerationTask(Long taskId) {
        try {
            CrawlerBlueprintGenerationTask task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalStateException("Generation task not found: " + taskId));
            String blueprintCode = task.blueprintCode();
            CrawlerBlueprintDraft draft = draftRepository.findByCode(blueprintCode)
                    .orElseThrow(() -> new IllegalStateException("Blueprint draft missing: " + blueprintCode));
            CrawlerBlueprintGenerationTask runningTask = taskRepository.save(task.markRunning(clock.instant()));

            Map<String, Object> payload = runningTask.inputPayload();
            String entryUrl = Objects.toString(payload.get(KEY_ENTRY_URL), draft.entryUrl());
            String keywords = Objects.toString(payload.get(KEY_SEARCH_KEYWORDS), "");
            String name = Objects.toString(payload.get(KEY_NAME), draft.name());
            String operator = Objects.toString(payload.get(KEY_OPERATOR), draft.generatedBy());

            Map<String, Object> snapshot = new LinkedHashMap<>();
            String pageHtml = fetchHtml(entryUrl, keywords, snapshot);
            snapshot.put("entryUrl", entryUrl);

            CrawlerBlueprintAutoParser.AutoParseResult parsed = autoParser.parse(entryUrl, pageHtml);
            CrawlerBlueprintValidator.ValidationResult validation = validator.validate(parsed.profile(), pageHtml);

            String configJson = configFactory.buildConfigJson(
                    entryUrl,
                    parsed.profile(),
                    parsed.pagingStrategy(),
                    parsed.automation(),
                    parsed.flow(),
                    parsed.metadata()
            );

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("success", validation.success());
            report.put("metrics", validation.metrics());
            report.put("warnings", validation.warnings());
            report.put("sampleData", validation.samples());
            report.put("snapshot", Map.of("url", snapshot.get("finalUrl")));

            String reportJson = writeJson(report);

            CrawlerBlueprintDraft updatedDraft = draft.refreshForGeneration(entryUrl, name, operator)
                    .withDraftResult(configJson, reportJson, clock.instant(), operator);
            draftRepository.save(updatedDraft);

            CrawlerBlueprintGenerationTask succeeded = runningTask.markSucceeded(clock.instant(), snapshot, validation.samples());
            taskRepository.save(succeeded);
            log.info("Blueprint generation succeeded for {}", blueprintCode);
        } catch (Exception ex) {
            log.warn("Blueprint generation failed for task {}: {}", taskId, ex.getMessage(), ex);
            handleFailure(taskId, ex);
        }
    }

    private void handleFailure(Long taskId, Exception ex) {
        taskRepository.findById(taskId).ifPresent(task -> {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("error", ex.getMessage());
            CrawlerBlueprintGenerationTask failed = task.markFailed(clock.instant(), ex.getMessage(), snapshot);
            taskRepository.save(failed);
            draftRepository.findByCode(task.blueprintCode()).ifPresent(draft -> {
                Map<String, Object> report = Map.of(
                        "success", false,
                        "error", ex.getMessage()
                );
                String reportJson = writeJson(report);
                CrawlerBlueprintDraft failedDraft = draft.markFailed(reportJson, draft.generatedBy());
                draftRepository.save(failedDraft);
            });
        });
    }

    private String fetchHtml(String entryUrl, String keywords, Map<String, Object> snapshot) throws Exception {
        return browserSessionManager.withPage(page -> {
            Page.NavigateOptions navigateOptions = new Page.NavigateOptions();
            navigateOptions.setWaitUntil(WaitUntilState.NETWORKIDLE);
            page.navigate(entryUrl, navigateOptions);
            waitForSettled(page);
            if (keywords != null && !keywords.isBlank()) {
                applySearch(page, keywords);
            }
            waitForSettled(page);
            snapshot.put("finalUrl", page.url());
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            snapshot.put("screenshot", Base64.getEncoder().encodeToString(screenshot));
            return page.content();
        });
    }

    private void applySearch(Page page, String keywords) {
        String[] selectors = new String[] {
                "input[type=search]",
                "input[name*=search i]",
                "input[placeholder*=search i]",
                "input[aria-label*=search i]",
                "input[type=text]"
        };
        for (String selector : selectors) {
            Locator locator = page.locator(selector);
            if (locator.count() > 0) {
                try {
                    locator.first().fill(keywords);
                    locator.first().press("Enter");
                    return;
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void waitForSettled(Page page) {
        try {
            page.waitForTimeout(1500);
        } catch (Exception ignored) {
        }
    }

    private String determineCode(GenerationCommand command) {
        if (command.blueprintCode() != null && !command.blueprintCode().isBlank()) {
            return command.blueprintCode().trim().toLowerCase(Locale.ROOT);
        }
        try {
            URIComponents components = URIComponents.from(command.entryUrl());
            if (!components.host().isBlank()) {
                return components.host().replaceAll("[^a-z0-9]", "-") + "-" + Long.toHexString(clock.millis());
            }
        } catch (Exception ignored) {
        }
        return "autogen-" + UUID.randomUUID().toString().replaceAll("[^a-z0-9]", "");
    }

    private String writeJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public record GenerationLaunchResult(CrawlerBlueprintDraft draft,
                                         CrawlerBlueprintGenerationTask task) {
    }

    public record GenerationCommand(String blueprintCode,
                                    String name,
                                    String entryUrl,
                                    String searchKeywords,
                                    List<String> excludeSelectors,
                                    String notes,
                                    String operatorEmail) {
    }

    private record URIComponents(String host) {
        static URIComponents from(String url) {
            if (url == null || url.isBlank()) {
                return new URIComponents("");
            }
            try {
                java.net.URI uri = new java.net.URI(url);
                return new URIComponents(Optional.ofNullable(uri.getHost()).orElse(""));
            } catch (Exception e) {
                return new URIComponents("");
            }
        }
    }
}
