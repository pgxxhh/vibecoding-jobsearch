package com.vibe.jobs.crawler.infrastructure.engine;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import com.vibe.jobs.crawler.infrastructure.config.CrawlerBrowserProperties;

@Component
public class BrowserSessionManager implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(BrowserSessionManager.class);

    private final Object lock = new Object();
    private final Semaphore playwrightInit = new Semaphore(1);
    private Playwright playwright;
    private Browser browser;
    private final CrawlerBrowserProperties properties;

    public BrowserSessionManager(CrawlerBrowserProperties properties) {
        this.properties = properties;
    }

    public <T> T withPage(PageCallback<T> callback) throws Exception {
        Objects.requireNonNull(callback, "callback");
        Browser activeBrowser = ensureBrowser();
        NewContextOptions options = new NewContextOptions()
                .setViewportSize(properties.getViewportWidth(), properties.getViewportHeight())
                .setUserAgent(randomUserAgent());
        Map<String, String> headers = properties.getExtraHeaders();
        if (!headers.isEmpty()) {
            options.setExtraHTTPHeaders(headers);
        }
        try (BrowserContext context = activeBrowser.newContext(options)) {
            for (String script : properties.getInitScripts()) {
                if (script != null && !script.isBlank()) {
                    context.addInitScript(script);
                }
            }
            try (Page page = context.newPage()) {
                page.setDefaultNavigationTimeout(toMillis(properties.getNavigationTimeout()));
                page.setDefaultTimeout(toMillis(properties.getDefaultTimeout()));
                return callback.apply(page);
            }
        }
    }

    private Browser ensureBrowser() throws Exception {
        if (browser != null) {
            return browser;
        }
        playwrightInit.acquire();
        try {
            if (browser != null) {
                return browser;
            }
            log.info("Starting shared Playwright browser instance for crawler automation");
            try {
                playwright = Playwright.create();
                BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();
                launchOptions.setHeadless(true);
                launchOptions.setArgs(Arrays.asList(
                    "--disable-dev-shm-usage", 
                    "--no-sandbox",
                    "--disable-setuid-sandbox",
                    "--disable-gpu",
                    "--disable-web-security",
                    "--disable-extensions"
                ));
                browser = playwright.chromium().launch(launchOptions);
                log.info("Playwright browser started successfully");
                return browser;
            } catch (Exception e) {
                log.error("Failed to start Playwright browser: {}", e.getMessage());
                // 清理资源
                if (browser != null) {
                    try { browser.close(); } catch (Exception ignored) {}
                    browser = null;
                }
                if (playwright != null) {
                    try { playwright.close(); } catch (Exception ignored) {}
                    playwright = null;
                }
                
                // 检查是否为依赖问题
                if (e.getMessage() != null && e.getMessage().contains("dependencies")) {
                    throw new RuntimeException("Playwright dependencies missing. Please install required system packages. " +
                                             "In Docker: apt-get install libx11-xcb1 libxcursor1 libgtk-3-0 libpangocairo-1.0-0 libcairo-gobject2 libgdk-pixbuf-2.0-0", e);
                }
                throw e;
            }
        } finally {
            playwrightInit.release();
        }
    }

    private String randomUserAgent() {
        List<String> agents = properties.getUserAgents();
        if (agents == null || agents.isEmpty()) {
            throw new IllegalStateException("No user agents configured for crawler browser context");
        }
        int index = ThreadLocalRandom.current().nextInt(agents.size());
        return agents.get(index);
    }

    private double toMillis(Duration duration) {
        return duration == null ? 0 : Math.max(0, duration.toMillis());
    }

    @Override
    @PreDestroy
    public void close() throws IOException {
        synchronized (lock) {
            if (browser != null) {
                try {
                    browser.close();
                } catch (RuntimeException ex) {
                    log.info("Failed to close browser: {}", ex.getMessage());
                }
                browser = null;
            }
            if (playwright != null) {
                try {
                    playwright.close();
                } catch (RuntimeException ex) {
                    log.info("Failed to close Playwright: {}", ex.getMessage());
                }
                playwright = null;
            }
        }
    }

    @FunctionalInterface
    public interface PageCallback<T> {
        T apply(Page page) throws Exception;
    }
}
