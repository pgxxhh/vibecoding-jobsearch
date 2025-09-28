package com.vibe.jobs.auth.infrastructure;

import com.vibe.jobs.auth.domain.VerificationCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RandomVerificationCodeGenerator implements VerificationCodeGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generate() {
        int value = RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
