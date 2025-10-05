package com.vibe.jobs.web.session;

import com.vibe.jobs.auth.application.EmailAuthService;
import com.vibe.jobs.auth.application.EmailAuthService.AuthenticatedUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

public class UserSessionInterceptor implements HandlerInterceptor {
    public static final String USER_PRINCIPAL_ATTR = UserPrincipal.class.getName();

    private final EmailAuthService emailAuthService;

    public UserSessionInterceptor(EmailAuthService emailAuthService) {
        this.emailAuthService = emailAuthService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String token = resolveToken(request);
        if (token == null) {
            reject(response, HttpStatus.UNAUTHORIZED);
            return false;
        }
        Optional<AuthenticatedUser> resolved = emailAuthService.resolveSession(token);
        if (resolved.isEmpty()) {
            reject(response, HttpStatus.UNAUTHORIZED);
            return false;
        }
        AuthenticatedUser user = resolved.get();
        request.setAttribute(USER_PRINCIPAL_ATTR, new UserPrincipal(user.userId(), user.email()));
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

    private void reject(HttpServletResponse response, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"UNAUTHORIZED\"}");
    }
}
