package com.vibe.jobs.crawler.infrastructure.engine;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages WebDriver instances for JavaScript rendering.
 * Provides driver pooling and lifecycle management to optimize performance.
 */
@Component
public class WebDriverManager {

    private static final Logger log = LoggerFactory.getLogger(WebDriverManager.class);
    private final ConcurrentHashMap<String, WebDriver> driverPool = new ConcurrentHashMap<>();
    private static final int MAX_POOL_SIZE = 5;

    /**
     * Get or create a WebDriver instance with specified options.
     */
    public WebDriver getDriver(String[] chromeOptions) {
        String key = String.join(",", chromeOptions);
        
        // Try to reuse existing driver from pool
        WebDriver driver = driverPool.get(key);
        if (driver != null && isDriverAlive(driver)) {
            return driver;
        }
        
        // Create new driver if none available or existing is dead
        if (driverPool.size() < MAX_POOL_SIZE) {
            driver = createNewDriver(chromeOptions);
            driverPool.put(key, driver);
            log.debug("Created new WebDriver instance for key: {}", key);
        } else {
            // Pool is full, create temporary driver
            driver = createNewDriver(chromeOptions);
            log.debug("Created temporary WebDriver instance (pool full)");
        }
        
        return driver;
    }

    /**
     * Release a WebDriver instance back to the pool or close it.
     */
    public void releaseDriver(WebDriver driver) {
        if (driver == null) return;
        
        try {
            // For temporary drivers (not in pool), close immediately
            if (!driverPool.containsValue(driver)) {
                driver.quit();
            }
            // Pooled drivers are kept alive for reuse
        } catch (Exception ex) {
            log.debug("Error releasing WebDriver: {}", ex.getMessage());
        }
    }

    private WebDriver createNewDriver(String[] customOptions) {
        ChromeOptions options = new ChromeOptions();
        
        // Default Chrome options for headless execution
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-web-security");
        options.addArguments("--disable-features=VizDisplayCompositor");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=" + randomUserAgent());
        
        // Disable CDP/DevTools to avoid version mismatch warnings
        options.addArguments("--disable-dev-tools");
        options.addArguments("--disable-extensions");
        options.addArguments("--remote-debugging-port=0");
        
        // Add custom options
        if (customOptions != null) {
            for (String option : customOptions) {
                if (option != null && !option.trim().isEmpty()) {
                    options.addArguments(option.trim());
                }
            }
        }
        
        return new ChromeDriver(options);
    }

    private boolean isDriverAlive(WebDriver driver) {
        try {
            driver.getCurrentUrl();
            return true;
        } catch (Exception ex) {
            return false;
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

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down WebDriver pool...");
        driverPool.values().forEach(driver -> {
            try {
                driver.quit();
            } catch (Exception ex) {
                log.debug("Error closing WebDriver during shutdown: {}", ex.getMessage());
            }
        });
        driverPool.clear();
    }
}