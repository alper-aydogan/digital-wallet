package com.alper.digitalwallet.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // Simple in-memory store: key=userId:endpoint, value=request count
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    
    private static final int REQUESTS_PER_MINUTE = 10;
    private static final int LOGIN_REQUESTS_PER_MINUTE = 5;
    private static final int TRANSFER_REQUESTS_PER_MINUTE = 3;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String remoteAddr = getClientIp(request);
        String endpoint = request.getRequestURI();
        
        int limit = getLimitForEndpoint(endpoint);
        String key = remoteAddr + ":" + endpoint;

        RateLimitBucket bucket = buckets.computeIfAbsent(key, k -> new RateLimitBucket(limit));

        if (!bucket.allowRequest()) {
            long retryAfter = bucket.getSecondsUntilReset();
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.getWriter().write("{\"status\": 429, \"code\": \"RATE_LIMIT_EXCEEDED\", \"message\": \"Too many requests. Retry after " + retryAfter + " seconds\"}");
            log.warn("Rate limit exceeded for {} on {}", remoteAddr, endpoint);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int getLimitForEndpoint(String endpoint) {
        if (endpoint.contains("/auth/login")) {
            return LOGIN_REQUESTS_PER_MINUTE;
        } else if (endpoint.contains("/transfer")) {
            return TRANSFER_REQUESTS_PER_MINUTE;
        }
        return REQUESTS_PER_MINUTE;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Simple bucket with sliding window
    private static class RateLimitBucket {
        private final int limit;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        RateLimitBucket(int limit) {
            this.limit = limit;
        }

        synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= 60_000) {
                count.set(0);
                windowStart = now;
            }

            if (count.get() < limit) {
                count.incrementAndGet();
                return true;
            }
            return false;
        }

        long getSecondsUntilReset() {
            return Math.max(0, (60_000 - (System.currentTimeMillis() - windowStart)) / 1000);
        }
    }
}

