package com.vibe.jobs.admin.interfaces;

import com.vibe.jobs.admin.application.AdminAccessService;
import com.vibe.jobs.admin.domain.AdminPrincipal;
import com.vibe.jobs.auth.application.EmailAuthService;
import com.vibe.jobs.auth.application.EmailAuthService.AuthenticatedUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

public class AdminAuthInterceptor implements HandlerInterceptor {

    static final String ADMIN_PRINCIPAL_ATTR = AdminPrincipal.class.getName();

    private final EmailAuthService emailAuthService;
    private final AdminAccessService accessService;

    public AdminAuthInterceptor(EmailAuthService emailAuthService, AdminAccessService accessService) {
        this.emailAuthService = emailAuthService;
        this.accessService = accessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {
        String token = resolveToken(request);
        if (token == null) {
            reject(response, HttpStatus.UNAUTHORIZED, "MISSING_SESSION");
            return false;
        }
        Optional<AuthenticatedUser> resolved = emailAuthService.resolveSession(token);
        if (resolved.isEmpty()) {
            reject(response, HttpStatus.UNAUTHORIZED, "INVALID_SESSION");
            return false;
        }
        String email = resolved.get().email();
        if (!accessService.isAllowed(email)) {
            reject(response, HttpStatus.FORBIDDEN, "NOT_ALLOWED");
            return false;
        }
        request.setAttribute(ADMIN_PRINCIPAL_ATTR, new AdminPrincipal(email));
        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("X-Session-Token");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String bearer = authHeader.substring(7).trim();
            if (!bearer.isEmpty()) {
                return bearer;
            }
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie != null && "vj_session".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if (value != null && !value.isBlank()) {
                        return value.trim();
                    }
                }
            }
        }
        return null;
    }

    private void reject(HttpServletResponse response, HttpStatus status, String code) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"" + code + "\"}");
    }
}
