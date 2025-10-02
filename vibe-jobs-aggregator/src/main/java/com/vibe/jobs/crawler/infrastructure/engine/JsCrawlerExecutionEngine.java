package com.vibe.jobs.crawler.infrastructure.engine;

import com.vibe.jobs.crawler.domain.CrawlBlueprint;
import com.vibe.jobs.crawler.domain.CrawlPagination;
import com.vibe.jobs.crawler.domain.CrawlSession;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Crawler execution engine that supports JavaScript-rendered pages using Selenium WebDriver.
 * This engine is suitable for Single Page Applications (SPAs) and sites with dynamic content.
 */
@Component
public class JsCrawlerExecutionEngine implements CrawlerExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(JsCrawlerExecutionEngine.class);
    private static final int DEFAULT_WAIT_SECONDS = 10;
    private static final int DEFAULT_PAGE_LOAD_TIMEOUT_SECONDS = 30;
    
    private final WebDriverManager webDriverManager;

    public JsCrawlerExecutionEngine(WebDriverManager webDriverManager) {
        this.webDriverManager = webDriverManager;
    }

    @Override
    public CrawlPageSnapshot fetch(CrawlSession session, CrawlPagination pagination) throws Exception {
        CrawlBlueprint blueprint = session.blueprint();
        String url = blueprint.resolveEntryUrl(session.context(), pagination);
        
        if (url == null || url.isBlank()) {
            return new CrawlPageSnapshot("", java.util.List.of(), Map.of("status", 400));
        }

        WebDriver driver = null;
        try {
            // Get custom Chrome options from blueprint configuration
            String customOptionsString = getConfigString(blueprint, "chromeOptions");
            String[] customOptions = customOptionsString != null && !customOptionsString.isBlank() 
                ? customOptionsString.split(",") 
                : new String[0];
                
            driver = webDriverManager.getDriver(customOptions);
            
            // Configure timeouts
            Duration pageLoadTimeout = Duration.ofSeconds(getConfigValue(blueprint, "pageLoadTimeoutSeconds", DEFAULT_PAGE_LOAD_TIMEOUT_SECONDS));
            Duration waitTimeout = Duration.ofSeconds(getConfigValue(blueprint, "waitSeconds", DEFAULT_WAIT_SECONDS));
            
            driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);
            
            log.debug("Fetching JS-rendered page {} for blueprint {}", pagination.page(), blueprint.code());
            
            // Navigate to the page
            driver.get(url);
            
            // Wait for dynamic content to load
            WebDriverWait wait = new WebDriverWait(driver, waitTimeout);
            String waitSelector = getConfigString(blueprint, "waitSelector");
            if (waitSelector != null && !waitSelector.isBlank()) {
                // Wait for specific element to be present
                wait.until(webDriver -> !webDriver.findElements(org.openqa.selenium.By.cssSelector(waitSelector)).isEmpty());
            } else {
                // Default wait for page to be in ready state
                wait.until(webDriver -> ((org.openqa.selenium.JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState").equals("complete"));
            }
            
            // Additional custom wait time if configured
            int additionalWaitMs = getConfigValue(blueprint, "additionalWaitMs", 0);
            if (additionalWaitMs > 0) {
                Thread.sleep(additionalWaitMs);
            }
            
            // Get page source after JavaScript execution
            String pageSource = driver.getPageSource();
            
            return new CrawlPageSnapshot(
                pageSource == null ? "" : pageSource, 
                java.util.List.of(), 
                Map.of(
                    "url", url,
                    "engine", "javascript",
                    "waitSelector", waitSelector != null ? waitSelector : "",
                    "pageTitle", driver.getTitle()
                )
            );
            
        } catch (Exception ex) {
            log.warn("JavaScript crawler execution failed for blueprint {} page {}: {}", 
                blueprint.code(), pagination.page(), ex.getMessage());
            throw ex;
        } finally {
            if (driver != null) {
                webDriverManager.releaseDriver(driver);
            }
        }
    }

    private String randomUserAgent() {
        String[] agents = new String[]{
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_2) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Safari/605.1.15",
                "Mozilla/5.0 (X11; Linux x86_64) Gecko/20100101 Firefox/124.0"
        };
        return agents[ThreadLocalRandom.current().nextInt(agents.length)];
    }
    
    private String getConfigString(CrawlBlueprint blueprint, String key) {
        return blueprint.metadata(key)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElse(null);
    }
    
    private int getConfigValue(CrawlBlueprint blueprint, String key, int defaultValue) {
        return blueprint.metadata(key)
                .filter(obj -> obj instanceof Number)
                .map(obj -> ((Number) obj).intValue())
                .orElse(defaultValue);
    }
}