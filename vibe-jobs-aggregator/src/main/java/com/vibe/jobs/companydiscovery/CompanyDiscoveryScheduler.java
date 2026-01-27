package com.vibe.jobs.companydiscovery;

import com.vibe.jobs.admin.application.CompanyDiscoverySettingsService;
import com.vibe.jobs.admin.domain.CompanyDiscoverySettingsSnapshot;
import com.vibe.jobs.admin.domain.event.CompanyDiscoverySettingsUpdatedEvent;
import com.vibe.jobs.companydiscovery.application.CompanyDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class CompanyDiscoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(CompanyDiscoveryScheduler.class);

    private final CompanyDiscoverySettingsService settingsService;
    private final CompanyDiscoveryService discoveryService;
    private final TaskScheduler taskScheduler;
    private volatile ScheduledFuture<?> scheduledTask;
    private final AtomicBoolean discoveryInProgress = new AtomicBoolean(false);

    public CompanyDiscoveryScheduler(CompanyDiscoverySettingsService settingsService,
                                     CompanyDiscoveryService discoveryService,
                                     TaskScheduler companyDiscoveryTaskScheduler) {
        this.settingsService = settingsService;
        this.discoveryService = discoveryService;
        this.taskScheduler = companyDiscoveryTaskScheduler;
        scheduleWith(settingsService.initializeIfNeeded());
    }

    @EventListener
    public void handleSettingsUpdated(CompanyDiscoverySettingsUpdatedEvent event) {
        if (event == null || event.snapshot() == null) {
            return;
        }
        scheduleWith(event.snapshot());
    }

    public void triggerImmediateRun(String actor) {
        taskScheduler.schedule(() -> runDiscoverySafely(actor), Instant.now());
    }

    private void scheduleWith(CompanyDiscoverySettingsSnapshot snapshot) {
        if (!snapshot.enabled()) {
            log.info("Company discovery disabled; cancelling scheduled task");
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
            }
            return;
        }
        Duration delay = Duration.ofMillis(Math.max(1_000L, snapshot.fixedDelayMs()));
        Instant start = Instant.now().plusMillis(Math.max(0L, snapshot.initialDelayMs()));
        synchronized (this) {
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
            }
            scheduledTask = taskScheduler.scheduleWithFixedDelay(() -> runDiscoverySafely(null), start, delay);
            log.info("Scheduled company discovery with initial delay {} ms and fixed delay {} ms", snapshot.initialDelayMs(), snapshot.fixedDelayMs());
        }
    }

    private void runDiscoverySafely(String actor) {
        if (!discoveryInProgress.compareAndSet(false, true)) {
            log.info("Company discovery already running; skipping trigger");
            return;
        }
        try {
            discoveryService.runDiscovery(actor);
        } catch (Exception ex) {
            log.error("Company discovery run failed", ex);
        } finally {
            discoveryInProgress.set(false);
        }
    }
}
