package com.vibe.jobs.auth.infrastructure;

import com.vibe.jobs.auth.spi.EmailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthInfrastructureConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmailSender.class)
    public EmailSender emailSender() {
        return new ConsoleEmailSender();
    }
}
