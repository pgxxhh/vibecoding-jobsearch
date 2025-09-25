
package com.vibe.jobs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AggregatorApplication {
    public static void main(String[] args) {
        System.out.println(System.getProperty("java.version"));
        SpringApplication.run(AggregatorApplication.class, args);
    }
}
