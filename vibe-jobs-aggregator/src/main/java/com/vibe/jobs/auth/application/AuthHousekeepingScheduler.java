package com.vibe.jobs.auth.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuthHousekeepingScheduler {
    private final AuthHousekeepingService housekeepingService;

    public AuthHousekeepingScheduler(AuthHousekeepingService housekeepingService) {
        this.housekeepingService = housekeepingService;
    }

    @Scheduled(fixedDelayString = "PT5M")
    public void runCleanup() {
        housekeepingService.cleanupExpired();
    }
}
