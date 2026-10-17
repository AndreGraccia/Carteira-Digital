package com.github.yildizmy.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Writes one row to {@code admin_audit} for every call to an endpoint guarded by {@code hasRole('ADMIN')},
 * allowed or denied. Method, path and status only: no body, no query string.
 */
@Slf4j
@Component
public class AdminAuditInterceptor implements HandlerInterceptor {

    static final int MAX_PATH = 200;

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public AdminAuditInterceptor(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod method) || !adminOnly(method)) return;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl user)) return;
        String path = request.getRequestURI();
        try {
            jdbc.update("INSERT INTO admin_audit (actor_id, method, path, status, at) VALUES (?, ?, ?, ?, ?)",
                    user.getId(),
                    request.getMethod(),
                    path.length() <= MAX_PATH ? path : path.substring(0, MAX_PATH),
                    response.getStatus(),
                    OffsetDateTime.now(clock));
        } catch (RuntimeException exception) {
            // The response is already sent; losing one audit row must be loud in the logs, not a user error.
            log.error("Admin audit row not written for {} {}", request.getMethod(), path);
        }
    }

    static boolean adminOnly(HandlerMethod method) {
        PreAuthorize rule = method.getMethodAnnotation(PreAuthorize.class);
        return rule != null && rule.value().contains("hasRole('ADMIN')");
    }
}
