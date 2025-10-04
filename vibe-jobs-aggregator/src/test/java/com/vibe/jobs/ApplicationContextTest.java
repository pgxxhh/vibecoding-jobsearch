package com.vibe.jobs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("h2")
class ApplicationContextTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring Boot application context loads successfully
        // with all the beans configured properly, including the admin settings.
    }
}