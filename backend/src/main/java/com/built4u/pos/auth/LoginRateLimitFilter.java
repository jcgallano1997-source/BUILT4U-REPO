package com.built4u.pos.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-IP fixed-window rate limiter for the login endpoint — a brute-force /
 * credential-stuffing brake that complements the per-account lockout in
 * {@code AuthService}. In-memory (single-node); a reverse proxy / WAF is the
 * real defence in a multi-node deploy, but this bounds abuse from one host.
 *
 * <p>Only guards {@code POST /api/auth/login}. Over the limit → 429 with a JSON
 * body and a {@code Retry-After} header. Windows are lazily reset per IP.
 */
@Component
@Slf4j
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final int maxAttempts;
    private final long windowMs;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(
        @Value("${app.security.login-rate.max-attempts:10}") int maxAttempts,
        @Value("${app.security.login-rate.window-seconds:900}") long windowSeconds
    ) {
        this.maxAttempts = maxAttempts;
        this.windowMs = windowSeconds * 1000L;
    }

    private static final class Window {
        volatile long startedAt;
        final AtomicInteger count = new AtomicInteger();
        Window(long now) { this.startedAt = now; }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String uri = req.getRequestURI();   // servletPath can be empty under MockMvc
        return !(HttpMethod.POST.matches(req.getMethod()) && uri != null && uri.endsWith("/api/auth/login"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String ip = clientIp(req);
        long now = System.currentTimeMillis();
        Window w = windows.computeIfAbsent(ip, k -> new Window(now));
        synchronized (w) {
            if (now - w.startedAt >= windowMs) {   // window elapsed → reset
                w.startedAt = now;
                w.count.set(0);
            }
        }
        if (w.count.incrementAndGet() > maxAttempts) {
            long retryAfter = Math.max(1, (windowMs - (now - w.startedAt)) / 1000);
            log.warn("Login rate limit hit for IP {} ({} attempts in window)", ip, w.count.get());
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(retryAfter));
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\","
                + "\"message\":\"Too many login attempts. Try again in " + retryAfter + " seconds.\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    /** First hop of X-Forwarded-For when behind a proxy, else the socket address. */
    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
