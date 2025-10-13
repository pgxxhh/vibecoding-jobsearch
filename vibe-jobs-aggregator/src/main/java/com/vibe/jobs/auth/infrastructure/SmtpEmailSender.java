package com.vibe.jobs.auth.infrastructure;

import com.vibe.jobs.auth.config.EmailAuthProperties;
import com.vibe.jobs.auth.domain.EmailAddress;
import com.vibe.jobs.auth.spi.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class SmtpEmailSender implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final EmailAuthProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, EmailAuthProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendVerificationCode(EmailAddress email, String code) {
        String from = resolveFromAddress();
        String subject = "Elaine Jobs 登录验证码";
        String text = buildBody(code);

        log.info("Attempting to send email to {} from {} using SMTP", email.masked(), from);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setTo(email.value());
            if (from != null && !from.isBlank()) {
                helper.setFrom(from);
            }
            helper.setSubject(subject);
            helper.setText(text, false);
            
            log.info("Sending email message via JavaMailSender...");
            mailSender.send(message);
            log.info("Successfully sent verification code email to {}", email.masked());
            return CompletableFuture.completedFuture(null);
        } catch (MailException ex) {
            log.error("Mail sending failed for {}: {}", email.masked(), ex.getMessage(), ex);
            return CompletableFuture.failedFuture(new IllegalStateException("SMTP mail sending failed: " + ex.getMessage(), ex));
        } catch (MessagingException ex) {
            log.error("Message creation failed for {}: {}", email.masked(), ex.getMessage(), ex);
            return CompletableFuture.failedFuture(new IllegalStateException("Email message creation failed: " + ex.getMessage(), ex));
        } catch (Exception ex) {
            log.error("Unexpected error sending email to {}: {}", email.masked(), ex.getMessage(), ex);
            return CompletableFuture.failedFuture(new IllegalStateException("Unexpected email sending error: " + ex.getMessage(), ex));
        }
    }

    private String resolveFromAddress() {
        if (properties.getFromAddress() != null && !properties.getFromAddress().isBlank()) {
            return properties.getFromAddress();
        }
        if (mailSender instanceof JavaMailSenderImpl impl) {
            return impl.getUsername();
        }
        return null;
    }

    private String buildBody(String code) {
        return "您好！\n\n您的登录验证码是：" + code + "。验证码有效期 10 分钟，请勿泄露给他人。\n\nElaine Jobs 团队";
    }
}
