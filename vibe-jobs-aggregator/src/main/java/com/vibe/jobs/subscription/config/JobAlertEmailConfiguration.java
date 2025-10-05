package com.vibe.jobs.subscription.config;

import com.vibe.jobs.auth.config.EmailAuthProperties;
import com.vibe.jobs.subscription.email.ConsoleJobAlertEmailSender;
import com.vibe.jobs.subscription.email.JobAlertEmailSender;
import com.vibe.jobs.subscription.email.SmtpJobAlertEmailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class JobAlertEmailConfiguration {

    @Bean
    @ConditionalOnProperty(name = "auth.email.sender", havingValue = "smtp", matchIfMissing = true)
    public JobAlertEmailSender smtpJobAlertEmailSender(JavaMailSender mailSender, EmailAuthProperties properties) {
        return new SmtpJobAlertEmailSender(mailSender, properties);
    }

    @Bean
    @ConditionalOnMissingBean(JobAlertEmailSender.class)
    public JobAlertEmailSender consoleJobAlertEmailSender() {
        return new ConsoleJobAlertEmailSender();
    }
}
