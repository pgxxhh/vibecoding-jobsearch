package com.vibe.jobs.subscription.email;

import com.vibe.jobs.auth.config.EmailAuthProperties;
import com.vibe.jobs.auth.domain.EmailAddress;
import com.vibe.jobs.subscription.service.JobAlertDigest;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class SmtpJobAlertEmailSender implements JobAlertEmailSender {
    private static final Logger log = LoggerFactory.getLogger(SmtpJobAlertEmailSender.class);

    private final JavaMailSender mailSender;
    private final EmailAuthProperties properties;

    public SmtpJobAlertEmailSender(JavaMailSender mailSender, EmailAuthProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendDigest(EmailAddress to, JobAlertDigest digest) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setTo(to.value());
            String from = resolveFromAddress();
            if (from != null && !from.isBlank()) {
                helper.setFrom(from);
            }
            helper.setSubject(buildSubject(digest));
            helper.setText(buildBody(digest), true);
            mailSender.send(message);
            log.info("Sent job alert digest email to {}", to.masked());
            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            log.error("Failed to send job alert email to {}", to.masked(), ex);
            return CompletableFuture.failedFuture(ex);
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

    private String buildSubject(JobAlertDigest digest) {
        return String.format("【Elaine Jobs】%d 个新的%s职位", digest.jobs().size(), digest.subscriptionSummary());
    }

    private String buildBody(JobAlertDigest digest) {
        StringBuilder html = new StringBuilder();
        html.append("<p>以下是根据您的订阅条件“").append(digest.subscriptionSummary()).append("”筛选出的最新职位：</p>");
        html.append("<ul>");
        digest.jobs().forEach(job -> html.append("<li><strong>")
                .append(job.title())
                .append("</strong> - ")
                .append(job.company())
                .append("（")
                .append(job.location() == null ? "不限地点" : job.location())
                .append("，发布于 ")
                .append(job.postedAt())
                .append("） <a href=\"")
                .append(job.url())
                .append("\">查看职位</a></li>"));
        html.append("</ul>");
        html.append("<p>如需取消订阅，请点击：<a href=\"")
                .append(digest.unsubscribeUrl())
                .append("\">取消订阅</a></p>");
        html.append("<p>感谢使用 Elaine Jobs。</p>");
        return html.toString();
    }
}
