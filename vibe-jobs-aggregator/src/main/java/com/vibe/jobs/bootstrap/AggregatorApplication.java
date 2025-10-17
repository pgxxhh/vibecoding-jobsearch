
package com.vibe.jobs.bootstrap;

import com.vibe.jobs.auth.config.EmailAuthProperties;
import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;
import com.vibe.jobs.shared.infrastructure.config.JobContentEnrichmentExecutorProperties;
import com.vibe.jobs.shared.infrastructure.config.JobDetailEnrichmentRetryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.vibe.jobs")
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({IngestionProperties.class,
        EmailAuthProperties.class,
        JobContentEnrichmentExecutorProperties.class,
        JobDetailEnrichmentRetryProperties.class})
public class AggregatorApplication {

    private static final Logger log = LoggerFactory.getLogger(AggregatorApplication.class);

    public static void main(String[] args) {
        log.info("Running on Java {}", System.getProperty("java.version"));
        SpringApplication.run(AggregatorApplication.class, args);
    }
}
