package com.vibe.jobs.web.session;

import com.vibe.jobs.auth.application.EmailAuthService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class UserWebMvcConfigurer implements WebMvcConfigurer {

    private final EmailAuthService emailAuthService;

    public UserWebMvcConfigurer(EmailAuthService emailAuthService) {
        this.emailAuthService = emailAuthService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserSessionInterceptor(emailAuthService))
                .addPathPatterns("/subscriptions/**")
                .excludePathPatterns("/subscriptions/*/unsubscribe");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UserPrincipalArgumentResolver());
    }
}
