package com.vibe.jobs.subscription.email;

import com.vibe.jobs.auth.domain.EmailAddress;
import com.vibe.jobs.subscription.service.JobAlertDigest;

import java.util.concurrent.CompletableFuture;

public interface JobAlertEmailSender {
    CompletableFuture<Void> sendDigest(EmailAddress to, JobAlertDigest digest);
}
