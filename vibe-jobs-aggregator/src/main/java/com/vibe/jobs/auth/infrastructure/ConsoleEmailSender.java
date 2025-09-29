package com.vibe.jobs.auth.infrastructure;

import com.vibe.jobs.auth.domain.EmailAddress;
import com.vibe.jobs.auth.spi.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

public class ConsoleEmailSender implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendVerificationCode(EmailAddress email, String code) {
        log.info("[DEV] Sending verification code {} to {}", code, email.value());
        return CompletableFuture.completedFuture(null);
    }
}
