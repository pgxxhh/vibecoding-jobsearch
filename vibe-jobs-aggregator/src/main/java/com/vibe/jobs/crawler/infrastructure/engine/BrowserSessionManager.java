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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Objects;
import java.util.concurrent.Semaphore;

@Component
public class BrowserSessionManager implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(BrowserSessionManager.class);
    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.113 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Safari/605.1.15",
            "Mozilla/5.0 (X11; Linux x86_64; rv:124.0) Gecko/20100101 Firefox/124.0"
    );

    private final Object lock = new Object();
    private final Semaphore playwrightInit = new Semaphore(1);
    private Playwright playwright;
    private Browser browser;

    public <T> T withPage(PageCallback<T> callback) throws Exception {
        Objects.requireNonNull(callback, "callback");
        Browser activeBrowser = ensureBrowser();
        NewContextOptions options = new NewContextOptions()
                .setViewportSize(1366, 768)
                .setUserAgent(randomUserAgent())
                .setExtraHTTPHeaders(Map.of(
                        "Accept-Language", "en-US,en;q=0.9",
                        "Upgrade-Insecure-Requests", "1"
                ));
        try (BrowserContext context = activeBrowser.newContext(options)) {
            context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined});");
            context.addInitScript("window.chrome = window.chrome || {runtime: {}};");
            context.addInitScript("Object.defineProperty(navigator, 'platform', {get: () => 'Win32'});");
            context.addInitScript("Object.defineProperty(navigator, 'plugins', {get: () => [1,2,3]});");
            try (Page page = context.newPage()) {
                page.setDefaultNavigationTimeout(90000);
                page.setDefaultTimeout(60000);
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
        if (USER_AGENTS.isEmpty()) {
            return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36";
        }
        int index = ThreadLocalRandom.current().nextInt(USER_AGENTS.size());
        return USER_AGENTS.get(index);
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
