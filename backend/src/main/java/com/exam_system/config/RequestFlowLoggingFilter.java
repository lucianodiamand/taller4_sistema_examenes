package com.exam_system.config;

import com.exam_system.auth.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class RequestFlowLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";

    private static final Logger logger = LoggerFactory.getLogger(RequestFlowLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String requestId = resolveRequestId(request);
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader("X-Request-Id", requestId);
        MDC.put(REQUEST_ID_ATTRIBUTE, requestId);

        Throwable error = null;
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException throwable) {
            error = throwable;
            throw throwable;
        } catch (Error throwable) {
            error = throwable;
            throw throwable;
        } finally {
            try {
                long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                String method = request.getMethod();
                String path = request.getRequestURI();
                int status = response.getStatus();
                String user = resolveUser();
                String role = resolveRole();
                String clientIp = resolveClientIp(request);

                if (error != null) {
                    logger.error(
                            "request-flow method={} path={} status={} durationMs={} user={} role={} ip={}",
                            method, path, status, durationMs, user, role, clientIp
                    );
                    return;
                }

                if (status == 401 || status == 403) {
                    logger.warn(
                            "request-flow method={} path={} status={} durationMs={} user={} role={} ip={}",
                            method, path, status, durationMs, user, role, clientIp
                    );
                    return;
                }

                if (status >= 400) {
                    return;
                }

                logger.info(
                        "request-flow method={} path={} status={} durationMs={} user={} role={} ip={}",
                        method, path, status, durationMs, user, role, clientIp
                );
            } finally {
                MDC.remove(REQUEST_ID_ATTRIBUTE);
            }
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }

        Object details = authentication.getDetails();
        if (details instanceof AuthenticatedUser authenticatedUser) {
            return String.valueOf(authenticatedUser.getId());
        }
        return authentication.getName();
    }

    private String resolveRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }

        Object details = authentication.getDetails();
        if (details instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.getRole();
        }
        return "unknown";
    }
}
