package com.vibe.jobs.admin.interfaces;

import com.vibe.jobs.admin.application.AdminAccessService;
import com.vibe.jobs.auth.application.EmailAuthService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class AdminWebMvcConfigurer implements WebMvcConfigurer {

    private final EmailAuthService emailAuthService;
    private final AdminAccessService accessService;

    public AdminWebMvcConfigurer(EmailAuthService emailAuthService, AdminAccessService accessService) {
        this.emailAuthService = emailAuthService;
        this.accessService = accessService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminAuthInterceptor(emailAuthService, accessService))
                .addPathPatterns("/admin/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AdminPrincipalArgumentResolver());
    }
}
