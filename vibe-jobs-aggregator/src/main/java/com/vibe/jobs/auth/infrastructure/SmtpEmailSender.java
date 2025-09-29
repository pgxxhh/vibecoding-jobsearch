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

import java.nio.charset.StandardCharsets;

public class SmtpEmailSender implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final EmailAuthProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, EmailAuthProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendVerificationCode(EmailAddress email, String code) {
        String from = resolveFromAddress();
        String subject = "Elaine Jobs 登录验证码";
        String text = buildBody(code);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setTo(email.value());
            if (from != null && !from.isBlank()) {
                helper.setFrom(from);
            }
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(message);
            log.info("Sent verification code email to {}", email.masked());
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send verification code to {}", email.value(), ex);
            throw new IllegalStateException("Failed to send verification email", ex);
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
