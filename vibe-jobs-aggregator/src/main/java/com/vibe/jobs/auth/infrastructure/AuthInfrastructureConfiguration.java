package com.vibe.jobs.auth.infrastructure;

import com.vibe.jobs.auth.config.EmailAuthProperties;
import com.vibe.jobs.auth.spi.EmailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class AuthInfrastructureConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.mail", name = "host")
    @ConditionalOnBean(JavaMailSender.class)
    public EmailSender smtpEmailSender(JavaMailSender mailSender,
                                       EmailAuthProperties emailAuthProperties,
                                       MailProperties mailProperties) {
        return new SmtpEmailSender(mailSender, emailAuthProperties, mailProperties);
    }

    @Bean
    @ConditionalOnMissingBean(EmailSender.class)
    public EmailSender emailSender() {
        return new ConsoleEmailSender();
    }
}
