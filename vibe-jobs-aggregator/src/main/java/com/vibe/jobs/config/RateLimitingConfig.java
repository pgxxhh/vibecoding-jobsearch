package com.vibe.jobs.config;

import com.vibe.jobs.web.rate.RateLimiterService;
import com.vibe.jobs.web.rate.RateLimitingInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitingConfig implements WebMvcConfigurer {

    private final RateLimitProperties properties;
    private final RateLimiterService rateLimiterService;

    public RateLimitingConfig(RateLimitProperties properties) {
        this.properties = properties;
        this.rateLimiterService = new RateLimiterService(properties.getWindow(), properties.getRequestsPerMinute());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (!properties.isEnabled()) {
            return;
        }
        RateLimitingInterceptor interceptor = new RateLimitingInterceptor(rateLimiterService);
        if (CollectionUtils.isEmpty(properties.getPaths())) {
            registry.addInterceptor(interceptor);
        } else {
            registry.addInterceptor(interceptor).addPathPatterns(properties.getPaths());
        }
    }
}

