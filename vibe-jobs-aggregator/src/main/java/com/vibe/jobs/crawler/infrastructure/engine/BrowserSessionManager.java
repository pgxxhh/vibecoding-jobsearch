package com.vibe.jobs.crawler.infrastructure.engine;

import com.microsoft.playwright.Browser;
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
import java.util.Objects;
import java.util.concurrent.Semaphore;

@Component
public class BrowserSessionManager implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(BrowserSessionManager.class);

    private final Object lock = new Object();
    private final Semaphore playwrightInit = new Semaphore(1);
    private Playwright playwright;
    private Browser browser;

    public <T> T withPage(PageCallback<T> callback) throws Exception {
        Objects.requireNonNull(callback, "callback");
        Browser activeBrowser = ensureBrowser();
        try (com.microsoft.playwright.BrowserContext context = activeBrowser.newContext();
             Page page = context.newPage()) {
            return callback.apply(page);
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
            playwright = Playwright.create();
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();
            launchOptions.setHeadless(true);
            launchOptions.setArgs(Arrays.asList("--disable-dev-shm-usage", "--no-sandbox"));
            browser = playwright.chromium().launch(launchOptions);
            return browser;
        } finally {
            playwrightInit.release();
        }
    }

    @Override
    @PreDestroy
    public void close() throws IOException {
        synchronized (lock) {
            if (browser != null) {
                try {
                    browser.close();
                } catch (RuntimeException ex) {
                    log.debug("Failed to close browser: {}", ex.getMessage());
                }
                browser = null;
            }
            if (playwright != null) {
                try {
                    playwright.close();
                } catch (RuntimeException ex) {
                    log.debug("Failed to close Playwright: {}", ex.getMessage());
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
