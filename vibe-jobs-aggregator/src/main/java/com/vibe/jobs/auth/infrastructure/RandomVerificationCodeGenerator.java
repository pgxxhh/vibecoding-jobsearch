package com.vibe.jobs.auth.infrastructure;

import com.vibe.jobs.auth.config.EmailAuthProperties;
import com.vibe.jobs.auth.domain.VerificationCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RandomVerificationCodeGenerator implements VerificationCodeGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    
    private final EmailAuthProperties properties;

    public RandomVerificationCodeGenerator(EmailAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public String generate() {
        // 如果启用了固定验证码模式（本地测试），返回固定验证码
        if (properties.isUseFixedCode()) {
            return properties.getFixedCode();
        }
        
        // 否则生成随机验证码
        int value = RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
