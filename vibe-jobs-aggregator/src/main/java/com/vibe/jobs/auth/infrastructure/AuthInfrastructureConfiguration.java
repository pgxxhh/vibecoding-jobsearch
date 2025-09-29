package com.vibe.jobs.auth.infrastructure;

import com.vibe.jobs.auth.config.EmailAuthProperties;
import com.vibe.jobs.auth.spi.EmailSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class AuthInfrastructureConfiguration {

    @Bean
    public EmailSender smtpEmailSender(JavaMailSender mailSender, EmailAuthProperties properties) {
        return new SmtpEmailSender(mailSender, properties);
    }
}
