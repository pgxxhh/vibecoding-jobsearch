package com.vibe.jobs.crawler.application.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.WaitForFunctionOptions;
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
import java.util.ArrayList;
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
    private static final List<String> JOB_FRAME_HINTS = List.of(
            "job",
            "career",
            "recruit",
            "workday",
            "greenhouse",
            "lever",
            "smartrecruiters",
            "bamboohr",
            "icims",
            "jobvite",
            "successfactors",
            "myworkdayjobs"
    );
    private static final List<String> SEARCH_INPUT_SELECTORS = List.of(
            "input[type=search]",
            "input[name*=search i]",
            "input[placeholder*=search i]",
            "input[aria-label*=search i]",
            "input[type=text]",
            "input[name*=keyword i]",
            "input[placeholder*=keyword i]"
    );
    private static final List<String> SEARCH_SUBMIT_SELECTORS = List.of(
            "button[type=submit]",
            "button[aria-label*=search i]",
            "button[data-testid*=search i]",
            "button[data-automation-id*=search i]",
            "[role=button][aria-label*=search i]",
            "input[type=submit]",
            "button[name*=search i]"
    );
    private static final String JOB_LIST_DETECTION_SCRIPT = """
            () => {
              const keywordHints = ['job','career','position','role','opening','vacancy','opportunity','职位','职位名称','岗位','职缺','招聘','机会','工作'];
              const selectors = [
                '[data-job-id]',
                '[data-jobid]',
                '[data-automation-id*=job i]',
                '[data-testid*=job i]',
                '[data-component*=job i]',
                '[data-qa*=job i]',
                '[data-ph-at-id*=job i]',
                '[role=listitem]',
                'article[data-automation*=job i]'
              ];
              const disallowed = new Set(['HEADER','FOOTER','NAV']);
              const isInNavigation = node => {
                if (!node) return false;
                if (disallowed.has(node.tagName)) return true;
                if (node.getAttribute && node.getAttribute('role') === 'navigation') return true;
                return !!(node.closest && node.closest('header, footer, nav, [role="navigation"]'));
              };
              const attrMatches = node => {
                if (!node) return false;
                const attributes = Array.from(node.attributes || []).map(attr => (attr.value || '').toLowerCase());
                return keywordHints.some(keyword => attributes.some(value => value.includes(keyword)));
              };
              const nodes = Array.from(document.querySelectorAll(selectors.join(',')));
              const jobNodes = nodes.filter(node => {
                if (!node || isInNavigation(node)) return false;
                if (attrMatches(node)) return true;
                const text = (node.textContent || '').toLowerCase();
                if (keywordHints.some(keyword => text.includes(keyword))) return true;
                const link = node.querySelector('a[href], button[role=link], [role=link]');
                if (link) {
                  const linkText = (link.textContent || '').toLowerCase();
                  const linkHref = (link.getAttribute('href') || '').toLowerCase();
                  if (keywordHints.some(keyword => linkText.includes(keyword) || linkHref.includes(keyword))) {
                    return true;
                  }
                }
                return false;
              });
              if (jobNodes.length >= 3) {
                const signatures = jobNodes.map(node => {
                  const target = node.closest('[data-job-id],[data-jobid],[role=listitem],li,article,section,div');
                  if (!target || isInNavigation(target)) return null;
                  const id = target.id || '';
                  const className = target.className || '';
                  return target.tagName + '|' + id + '|' + className;
                }).filter(Boolean);
                if (signatures.length >= 3) {
                  const counts = signatures.reduce((acc, key) => {
                    acc[key] = (acc[key] || 0) + 1;
                    return acc;
                  }, {});
                  if (Object.values(counts).some(count => count >= 3)) {
                    return true;
                  }
                }
              }
              const anchors = Array.from(document.querySelectorAll('a[href], button[role=link], [role=link]')).filter(node => {
                if (!node || isInNavigation(node)) return false;
                const text = (node.textContent || '').toLowerCase();
                const href = (node.getAttribute('href') || '').toLowerCase();
                return keywordHints.some(keyword => text.includes(keyword) || href.includes(keyword));
              });
              if (anchors.length >= 3) {
                const anchorSignatures = anchors.map(node => {
                  const target = node.closest('li, article, section, div');
                  if (!target || isInNavigation(target)) return null;
                  const id = target.id || '';
                  const className = target.className || '';
                  return target.tagName + '|' + id + '|' + className;
                }).filter(Boolean);
                const counts = anchorSignatures.reduce((acc, key) => {
                  acc[key] = (acc[key] || 0) + 1;
                  return acc;
                }, {});
                if (Object.values(counts).some(count => count >= 3)) {
                  return true;
                }
              }
              return false;
            }
            """;

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
            snapshot.put("listSelector", parsed.profile().listSelector());
            if (parsed.metadata() != null && !parsed.metadata().isEmpty()) {
                snapshot.put("parserMetadata", parsed.metadata());
            }
            CrawlerBlueprintValidator.ValidationResult validation = validator.validate(parsed.profile(), pageHtml);
            if (!validation.success()) {
                throw new IllegalStateException("Generated parser did not produce any job listings");
            }

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
        List<String> warnings = new ArrayList<>();
        String html = browserSessionManager.withPage(page -> {
            Page.NavigateOptions navigateOptions = new Page.NavigateOptions();
            navigateOptions.setWaitUntil(WaitUntilState.NETWORKIDLE);
            page.navigate(entryUrl, navigateOptions);
            waitForSettled(page);

            SearchOutcome searchOutcome = SearchOutcome.noAttempt();
            if (keywords != null && !keywords.isBlank()) {
                searchOutcome = applySearch(page, keywords, warnings);
                if (searchOutcome.attempted()) {
                    snapshot.put("search", searchOutcome.toSnapshot(keywords));
                }
            }

            waitForSettled(page);
            waitForJobListings(page, warnings);
            waitForSettled(page);

            String pageUrl = page.url();
            snapshot.put("pageUrl", pageUrl);

            PageContext context = resolvePageContext(page);
            snapshot.put("finalUrl", context.finalUrl());
            context.frameUrl().ifPresent(url -> snapshot.put("frameUrl", url));

            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            snapshot.put("screenshot", Base64.getEncoder().encodeToString(screenshot));
            return context.html();
        });
        snapshot.put("warnings", warnings.isEmpty() ? List.of() : List.copyOf(warnings));
        return html;
    }

    private PageContext resolvePageContext(Page page) {
        String html = page.content();
        String finalUrl = page.url();
        Frame jobFrame = locateJobFrame(page);
        if (jobFrame == null) {
            return new PageContext(html, finalUrl, Optional.empty());
        }
        try {
            page.waitForTimeout(750);
        } catch (RuntimeException ignored) {
        }
        try {
            String frameHtml = jobFrame.content();
            if (frameHtml != null && !frameHtml.isBlank()) {
                String frameUrl = jobFrame.url();
                if (frameUrl != null && !frameUrl.isBlank()) {
                    finalUrl = frameUrl;
                }
                return new PageContext(frameHtml, finalUrl, Optional.ofNullable(frameUrl));
            }
        } catch (RuntimeException ex) {
            log.debug("Failed to extract iframe content: {}", ex.getMessage());
        }
        return new PageContext(html, finalUrl, Optional.ofNullable(jobFrame.url()));
    }

    private Frame locateJobFrame(Page page) {
        return page.frames().stream()
                .filter(frame -> frame != null && frame != page.mainFrame())
                .filter(frame -> isJobFrame(frame.url()) || isJobFrame(frame.name()))
                .findFirst()
                .orElse(null);
    }

    private boolean isJobFrame(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String hint : JOB_FRAME_HINTS) {
            if (normalized.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private SearchOutcome applySearch(Page page, String keywords, List<String> warnings) {
        if (keywords == null || keywords.isBlank()) {
            return SearchOutcome.noAttempt();
        }
        String trimmed = keywords.trim();
        Locator activeInput = null;
        String inputSelector = null;
        for (String selector : SEARCH_INPUT_SELECTORS) {
            Locator locator = page.locator(selector);
            if (locator.count() == 0) {
                continue;
            }
            try {
                Locator field = locator.first();
                field.click();
                field.fill("");
                field.type(trimmed);
                activeInput = field;
                inputSelector = selector;
                break;
            } catch (RuntimeException ex) {
                log.debug("Search input interaction failed for {}: {}", selector, ex.getMessage());
            }
        }
        if (activeInput == null) {
            warnings.add("Search keywords provided but no search input detected on page.");
            return new SearchOutcome(true, false, null, null, "input-not-found");
        }

        boolean submitted = false;
        String submitSelector = null;
        for (String selector : SEARCH_SUBMIT_SELECTORS) {
            Locator submit = page.locator(selector);
            if (submit.count() == 0) {
                continue;
            }
            try {
                submit.first().click();
                submitted = true;
                submitSelector = selector;
                break;
            } catch (RuntimeException ex) {
                log.debug("Search submit click failed for {}: {}", selector, ex.getMessage());
            }
        }
        if (!submitted) {
            try {
                activeInput.press("Enter");
                submitted = true;
                submitSelector = "EnterKey";
            } catch (RuntimeException ex) {
                log.debug("Search submit via Enter failed: {}", ex.getMessage());
            }
        }
        if (!submitted) {
            try {
                Boolean formSubmitted = (Boolean) activeInput.evaluate(
                    "(el) => { if (el && el.form) { el.form.requestSubmit(); return true; } const button = el.closest('form')?.querySelector('button[type=submit],input[type=submit]'); if (button) { button.click(); return true; } return false; }"
                );
                if (Boolean.TRUE.equals(formSubmitted)) {
                    submitted = true;
                    submitSelector = "form.requestSubmit";
                }
            } catch (RuntimeException ex) {
                log.debug("Search submit via form fallback failed: {}", ex.getMessage());
            }
        }
        if (!submitted) {
            warnings.add("Filled search keywords but did not detect a submit action; results may be unfiltered.");
        }
        return new SearchOutcome(true, submitted, inputSelector, submitSelector, submitted ? null : "submit-not-triggered");
    }

    private void waitForSettled(Page page) {
        try {
            page.waitForTimeout(1500);
        } catch (Exception ignored) {
        }
    }

    private void waitForJobListings(Page page, List<String> warnings) {
        if (waitForJobListingsInternal(page, JOB_LIST_DETECTION_SCRIPT, 9000)) {
            return;
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                page.evaluate("() => window.scrollBy(0, Math.max(window.innerHeight, 600))");
            } catch (RuntimeException ex) {
                log.debug("Failed to scroll while waiting for job listings: {}", ex.getMessage());
            }
            waitForSettled(page);
            if (waitForJobListingsInternal(page, JOB_LIST_DETECTION_SCRIPT, 6000)) {
                return;
            }
        }
        warnings.add("Unable to confirm repeating job listings automatically; captured snapshot for manual review.");
    }

    private boolean waitForJobListingsInternal(Page page, String script, double timeoutMs) {
        try {
            WaitForFunctionOptions options = new WaitForFunctionOptions();
            options.setTimeout(timeoutMs);
            page.waitForFunction(script, null, options);
            return true;
        } catch (RuntimeException ex) {
            log.debug("Timed out waiting for job listings to stabilize: {}", ex.getMessage());
            return false;
        }
    }

    private record PageContext(String html, String finalUrl, Optional<String> frameUrl) {
    }

    private record SearchOutcome(boolean attempted,
                                 boolean submitted,
                                 String inputSelector,
                                 String submitSelector,
                                 String failureReason) {

        static SearchOutcome noAttempt() {
            return new SearchOutcome(false, false, null, null, null);
        }

        Map<String, Object> toSnapshot(String keywords) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("keywords", keywords);
            data.put("attempted", attempted);
            data.put("submitted", submitted);
            if (inputSelector != null) {
                data.put("inputSelector", inputSelector);
            }
            if (submitSelector != null) {
                data.put("submitSelector", submitSelector);
            }
            if (failureReason != null) {
                data.put("failureReason", failureReason);
            }
            return data;
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
