package com.vibe.jobs.crawler.infrastructure.engine;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.NavigateOptions;
import com.microsoft.playwright.Page.WaitForSelectorOptions;
import com.microsoft.playwright.options.WaitUntilState;
import com.vibe.jobs.crawler.domain.AutomationSettings;
import com.vibe.jobs.crawler.domain.CrawlBlueprint;
import com.vibe.jobs.crawler.domain.CrawlFlow;
import com.vibe.jobs.crawler.domain.CrawlPagination;
import com.vibe.jobs.crawler.domain.CrawlSession;
import com.vibe.jobs.crawler.domain.CrawlStep;
import com.vibe.jobs.crawler.domain.CrawlStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class BrowserCrawlerExecutionEngine implements CrawlerExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(BrowserCrawlerExecutionEngine.class);

    private final BrowserSessionManager sessionManager;

    public BrowserCrawlerExecutionEngine(BrowserSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public boolean supports(CrawlBlueprint blueprint) {
        return blueprint != null && blueprint.requiresBrowser();
    }

    @Override
    public CrawlPageSnapshot fetch(CrawlSession session, CrawlPagination pagination) throws Exception {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(pagination, "pagination");
        CrawlBlueprint blueprint = session.blueprint();
        String url = blueprint.resolveEntryUrl(session.context(), pagination);
        if (url == null || url.isBlank()) {
            return new CrawlPageSnapshot("", List.of(), Map.of("status", 400));
        }
        return sessionManager.withPage(page -> execute(session, pagination, url, page));
    }

    private CrawlPageSnapshot execute(CrawlSession session, CrawlPagination pagination, String url, Page page) throws Exception {
        NavigateOptions navigateOptions = new NavigateOptions();
        navigateOptions.setWaitUntil(WaitUntilState.DOMCONTENTLOADED);
        log.info("[{}] Navigating to {} with browser engine", session.blueprint().code(), url);
        page.navigate(url, navigateOptions);
        AutomationSettings automation = session.blueprint().automation();
        applyAutomation(page, automation, session);
        FlowExecutionResult result = runFlow(page, session, pagination);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("url", page.url());
        metadata.put("engine", "browser");
        metadata.put("page", pagination.page());
        return new CrawlPageSnapshot(result.listHtml(), result.detailHtml(), metadata);
    }

    private void applyAutomation(Page page, AutomationSettings automation, CrawlSession session) {
        if (automation == null || !automation.enabled()) {
            return;
        }
        try {
            if (!automation.waitForSelector().isBlank()) {
                page.waitForSelector(automation.waitForSelector(), new WaitForSelectorOptions().setTimeout(automation.waitForMilliseconds() <= 0 ? 30000 : automation.waitForMilliseconds()));
            } else if (automation.waitForMilliseconds() > 0) {
                page.waitForTimeout(automation.waitForMilliseconds());
            }
        } catch (RuntimeException ex) {
            log.info("Automation pre-wait failed: {}", ex.getMessage());
        }
        AutomationSettings.SearchSettings search = automation.search();
        if (search == null || !search.enabled()) {
            return;
        }
        for (AutomationSettings.SearchField field : search.fields()) {
            if (field == null || field.selector().isBlank()) {
                continue;
            }
            String value = field.constantValue();
            if ((value == null || value.isBlank()) && !field.optionKey().isBlank()) {
                value = session.context().option(field.optionKey());
            }
            if ((value == null || value.isBlank()) && field.required()) {
                log.info("Skipping automation field {} due to missing value", field.selector());
                continue;
            }
            try {
                Locator locator = page.locator(field.selector());
                switch (field.strategy()) {
                    case SELECT -> {
                        if (value != null && !value.isBlank()) {
                            locator.selectOption(new String[]{value});
                        }
                    }
                    case CLICK -> locator.click();
                    case FILL -> {
                        if (field.clearBefore()) {
                            locator.fill("");
                        }
                        if (value != null) {
                            locator.fill(value);
                        }
                    }
                }
            } catch (RuntimeException ex) {
                log.info("Failed to execute search field action on {}: {}", field.selector(), ex.getMessage());
            }
        }
        if (!search.submitSelector().isBlank()) {
            try {
                page.locator(search.submitSelector()).first().click();
            } catch (RuntimeException ex) {
                log.info("Failed to click submit selector {}: {}", search.submitSelector(), ex.getMessage());
            }
        }
        try {
            if (!search.waitForSelector().isBlank()) {
                page.waitForSelector(search.waitForSelector(), new WaitForSelectorOptions().setTimeout(search.waitAfterSubmitMs() <= 0 ? 30000 : search.waitAfterSubmitMs()));
            } else if (search.waitAfterSubmitMs() > 0) {
                page.waitForTimeout(search.waitAfterSubmitMs());
            }
        } catch (RuntimeException ex) {
            log.info("Automation post-wait failed: {}", ex.getMessage());
        }
    }

    private FlowExecutionResult runFlow(Page page, CrawlSession session, CrawlPagination pagination) {
        CrawlFlow flow = session.blueprint().flow();
        if (flow == null || flow.isEmpty()) {
            return new FlowExecutionResult(page.content(), List.of());
        }
        String listHtml = page.content();
        List<String> detailHtml = new ArrayList<>();
        for (CrawlStep step : flow.steps()) {
            Map<String, Object> options = step.options();
            try {
                switch (step.type()) {
                    case REQUEST -> {
                        String nextUrl = asString(options.get("url"));
                        if (!nextUrl.isBlank()) {
                            String resolved = resolveUrl(page.url(), nextUrl);
                            page.navigate(resolved, new NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                            listHtml = page.content();
                        }
                    }
                    case WAIT -> {
                        handleWait(page, options);
                    }
                    case SCROLL -> {
                        handleScroll(page, options);
                    }
                    case CLICK -> {
                        handleClick(page, options);
                        listHtml = page.content();
                    }
                    case EXTRACT_LIST -> {
                        listHtml = page.content();
                    }
                    case EXTRACT_DETAIL -> {
                        detailHtml.addAll(collectDetailPages(page, options));
                    }
                    default -> {
                    }
                }
            } catch (RuntimeException ex) {
                log.info("Failed to execute crawl step {} for blueprint {}: {}", step.type(), session.blueprint().code(), ex.getMessage());
            }
        }
        return new FlowExecutionResult(listHtml, detailHtml);
    }

    private void handleWait(Page page, Map<String, Object> options) {
        String selector = asString(options.get("selector"));
        int duration = asInteger(options.get("durationMs"), 0);
        
        if (!selector.isBlank()) {
            // 增加超时时间，特别是对于SPA网站
            int timeout = duration > 0 ? Math.min(duration, 60000) : 45000; // 最长45秒
            try {
                page.waitForSelector(selector, new WaitForSelectorOptions().setTimeout(timeout));
            } catch (RuntimeException ex) {
                log.info("Wait for selector '{}' timed out after {}ms: {}", selector, timeout, ex.getMessage());
                // 即使选择器等待失败，也尝试等待固定时间以防页面仍在加载
                if (duration > 0) {
                    page.waitForTimeout(Math.min(duration, 10000));
                }
            }
        } else if (duration > 0) {
            page.waitForTimeout(Math.min(duration, 30000)); // 最长30秒固定等待
        }
    }

    private void handleScroll(Page page, Map<String, Object> options) {
        String to = asString(options.get("to"));
        if ("bottom".equalsIgnoreCase(to)) {
            page.evaluate("window.scrollTo(0, document.body.scrollHeight);");
            return;
        }
        int times = Math.max(1, asInteger(options.get("times"), 1));
        int pixels = Math.max(0, asInteger(options.get("pixels"), 500));
        for (int i = 0; i < times; i++) {
            page.mouse().wheel(0, pixels);
            page.waitForTimeout(200);
        }
    }

    private void handleClick(Page page, Map<String, Object> options) {
        String selector = asString(options.get("selector"));
        if (!selector.isBlank()) {
            page.locator(selector).first().click();
        }
    }

    private List<String> collectDetailPages(Page page, Map<String, Object> options) {
        String selector = asString(options.get("selector"));
        int limit = Math.max(0, asInteger(options.get("limit"), 10));
        if (selector.isBlank() || limit == 0) {
            return List.of();
        }
        List<String> snapshots = new ArrayList<>();
        Locator locator = page.locator(selector);
        int count = locator.count();
        for (int i = 0; i < count && snapshots.size() < limit; i++) {
            String href = locator.nth(i).getAttribute("href");
            if (href == null || href.isBlank()) {
                continue;
            }
            String resolved = resolveUrl(page.url(), href);
            try (Page detail = page.context().newPage()) {
                detail.navigate(resolved, new NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                snapshots.add(detail.content());
            } catch (RuntimeException ex) {
                log.info("Failed to collect detail page {}: {}", resolved, ex.getMessage());
            }
        }
        return snapshots;
    }

    private String resolveUrl(String base, String href) {
        if (href == null || href.isBlank()) {
            return base;
        }
        try {
            URI hrefUri = new URI(href);
            if (hrefUri.isAbsolute()) {
                return hrefUri.toString();
            }
            URI baseUri = new URI(base == null ? "" : base);
            return baseUri.resolve(hrefUri).toString();
        } catch (URISyntaxException e) {
            return href;
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String s) {
            return s.trim();
        }
        return String.valueOf(value).trim();
    }

    private int asInteger(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private record FlowExecutionResult(String listHtml, List<String> detailHtml) {
    }
}
