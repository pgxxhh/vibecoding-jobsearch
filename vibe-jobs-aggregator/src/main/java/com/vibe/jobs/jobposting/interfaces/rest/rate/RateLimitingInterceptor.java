package com.vibe.jobs.jobposting.interfaces.rest.rate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitingInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String key = resolveKey(request);
        if (!rateLimiterService.tryAcquire(key)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}");
            return false;
        }
        return true;
    }

    private String resolveKey(HttpServletRequest request) {
        if (request.getUserPrincipal() != null) {
            return "user:" + request.getUserPrincipal().getName();
        }
        return "ip:" + request.getRemoteAddr();
    }
}

