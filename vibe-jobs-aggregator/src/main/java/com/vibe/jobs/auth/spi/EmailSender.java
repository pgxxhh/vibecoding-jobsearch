package com.vibe.jobs.auth.spi;

import com.vibe.jobs.auth.domain.EmailAddress;

import java.util.concurrent.CompletableFuture;

public interface EmailSender {
    CompletableFuture<Void> sendVerificationCode(EmailAddress email, String code);
}
