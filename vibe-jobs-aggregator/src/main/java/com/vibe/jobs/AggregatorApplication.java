
package com.vibe.jobs;

import com.vibe.jobs.auth.config.EmailAuthProperties;
import com.vibe.jobs.config.IngestionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({IngestionProperties.class, EmailAuthProperties.class})
public class AggregatorApplication {
    public static void main(String[] args) {
        System.out.println(System.getProperty("java.version"));
        SpringApplication.run(AggregatorApplication.class, args);
    }
}
