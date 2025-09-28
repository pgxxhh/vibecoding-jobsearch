package com.vibe.jobs.auth.spi;

import com.vibe.jobs.auth.domain.EmailAddress;

public interface EmailSender {
    void sendVerificationCode(EmailAddress email, String code);
}
