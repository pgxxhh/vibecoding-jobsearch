package com.vibe.jobs.subscription.service;

import com.vibe.jobs.subscription.domain.JobAlertSubscription;
import com.vibe.jobs.subscription.domain.JobAlertSubscriptionStatus;
import com.vibe.jobs.subscription.repo.JobAlertSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JobAlertScheduler {
    private static final Logger log = LoggerFactory.getLogger(JobAlertScheduler.class);

    private final JobAlertSubscriptionRepository subscriptionRepository;
    private final JobAlertSubscriptionService subscriptionService;
    private final Clock clock;

    public JobAlertScheduler(JobAlertSubscriptionRepository subscriptionRepository,
                             JobAlertSubscriptionService subscriptionService,
                             Optional<Clock> clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
        this.clock = clock.orElse(Clock.systemUTC());
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void dispatchDigests() {
        Instant now = Instant.now(clock);
        List<JobAlertSubscription> activeSubscriptions = subscriptionRepository.findByStatus(JobAlertSubscriptionStatus.ACTIVE);
        activeSubscriptions.stream()
                .filter(subscription -> subscriptionService.shouldRun(subscription, now))
                .forEach(subscription -> {
                    try {
                        subscriptionService.processSubscription(subscription);
                    } catch (Exception ex) {
                        log.error("Failed to process subscription {}", subscription.getId(), ex);
                    }
                });
    }
}
