package com.vibe.jobs.subscription.email;

import com.vibe.jobs.auth.domain.EmailAddress;
import com.vibe.jobs.subscription.service.JobAlertDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

public class ConsoleJobAlertEmailSender implements JobAlertEmailSender {
    private static final Logger log = LoggerFactory.getLogger(ConsoleJobAlertEmailSender.class);

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendDigest(EmailAddress to, JobAlertDigest digest) {
        log.info("[ConsoleEmail] Would send job alert to {} with {} jobs", to.masked(), digest.jobs().size());
        digest.jobs().forEach(job -> log.info(" - {} @ {} ({})", job.title(), job.company(), job.location()));
        return CompletableFuture.completedFuture(null);
    }
}
