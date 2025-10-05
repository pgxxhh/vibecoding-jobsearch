
package com.vibe.jobs;

import com.vibe.jobs.auth.config.EmailAuthProperties;
import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.subscription.config.JobAlertProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({IngestionProperties.class, EmailAuthProperties.class, JobAlertProperties.class})
public class AggregatorApplication {
    public static void main(String[] args) {
        System.out.println(System.getProperty("java.version"));
        SpringApplication.run(AggregatorApplication.class, args);
    }
}
