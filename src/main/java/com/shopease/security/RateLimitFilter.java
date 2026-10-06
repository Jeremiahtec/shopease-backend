package com.shopease.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopease.config.RateLimitProperties;
import com.shopease.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing and signup/reset abuse: each client IP may call each sensitive endpoint only
 * N times per window. In-memory, so it applies per application instance (fine for a single server).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED = Set.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/refresh",
            "/api/auth/forgot-password", "/api/auth/reset-password");

    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !LIMITED.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long windowMs = props.windowSeconds() * 1000L;
        long now = System.currentTimeMillis();
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();
        Deque<Long> queue = hits.computeIfAbsent(key, k -> new ArrayDeque<>());

        long retryAfterSeconds = -1;
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst() < now - windowMs) {
                queue.pollFirst();
            }
            if (queue.size() >= props.maxRequests()) {
                retryAfterSeconds = (queue.peekFirst() + windowMs - now) / 1000 + 1;
            } else {
                queue.addLast(now);
            }
        }

        if (retryAfterSeconds >= 0) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), new ApiError(LocalDateTime.now(), 429,
                    "Too Many Requests", "Too many attempts. Please wait " + retryAfterSeconds + " seconds and try again.",
                    request.getRequestURI(), null));
            return;
        }
        chain.doFilter(request, response);
    }

    /** Forget clients that have been quiet for a while so the map cannot grow forever. */
    @Scheduled(fixedDelay = 300_000)
    public void cleanup() {
        long cutoff = System.currentTimeMillis() - props.windowSeconds() * 1000L;
        hits.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                Long last = entry.getValue().peekLast();
                return last == null || last < cutoff;
            }
        });
    }
}
