package com.vibe.jobs.auth.infrastructure;

import com.vibe.jobs.auth.config.EmailAuthProperties;
import com.vibe.jobs.auth.domain.EmailAddress;
import com.vibe.jobs.auth.spi.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;

import java.time.Duration;

public class SmtpEmailSender implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final EmailAuthProperties emailAuthProperties;
    private final MailProperties mailProperties;

    public SmtpEmailSender(JavaMailSender mailSender,
                           EmailAuthProperties emailAuthProperties,
                           MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.emailAuthProperties = emailAuthProperties;
        this.mailProperties = mailProperties;
    }

    @Override
    public void sendVerificationCode(EmailAddress email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolveFromAddress());
        message.setTo(email.value());
        message.setSubject("Your VibeCoding verification code");
        message.setText(buildBody(code));

        try {
            mailSender.send(message);
            if (log.isDebugEnabled()) {
                log.debug("Sent verification code email to {}", email.value());
            }
        } catch (MailException ex) {
            log.error("Failed to send verification code email to {}", email.value(), ex);
            throw ex;
        }
    }

    private String resolveFromAddress() {
        if (StringUtils.hasText(emailAuthProperties.getSenderAddress())) {
            return emailAuthProperties.getSenderAddress();
        }
        if (StringUtils.hasText(mailProperties.getUsername())) {
            return mailProperties.getUsername();
        }
        throw new IllegalStateException("Sender email address is not configured. Set auth.email.senderAddress or spring.mail.username.");
    }

    private String buildBody(String code) {
        Duration ttl = emailAuthProperties.getChallengeTtl();
        long minutes = ttl.toMinutes();
        if (minutes <= 0) {
            minutes = Math.max(1, ttl.getSeconds() / 60);
        }

        String ttlMessage = minutes > 0
                ? "This code will expire in " + minutes + " minute" + (minutes == 1 ? "" : "s") + "."
                : "This code will expire soon.";

        return "Hello,\n\n" +
                "Your verification code is: " + code + "\n" +
                ttlMessage + "\n\n" +
                "If you did not request this code, you can safely ignore this email.";
    }
}
