package com.vibe.jobs.auth.infrastructure;

import com.vibe.jobs.auth.config.EmailAuthProperties;
import com.vibe.jobs.auth.spi.EmailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class AuthInfrastructureConfiguration {

    @Bean
    @ConditionalOnProperty(name = "auth.email.sender", havingValue = "smtp", matchIfMissing = true)
    public EmailSender smtpEmailSender(JavaMailSender mailSender, EmailAuthProperties properties) {
        return new SmtpEmailSender(mailSender, properties);
    }

    @Bean
    @ConditionalOnProperty(name = "auth.email.sender", havingValue = "console")
    public EmailSender consoleEmailSender() {
        return new ConsoleEmailSender();
    }
}
