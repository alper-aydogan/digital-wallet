package com.alper.digitalwallet.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
@Profile("!test")
@ConditionalOnProperty(prefix = "app.rate-limiting", name = "enabled", havingValue = "true")
@ConditionalOnBean(ProxyManager.class)
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ProxyManager<byte[]> proxyManager;

    private static final int REQUESTS_PER_MINUTE = 10;
    private static final int LOGIN_REQUESTS_PER_MINUTE = 5;
    private static final int TRANSFER_REQUESTS_PER_MINUTE = 3;

    @Value("${app.trust-proxy:false}")
    private boolean trustProxy;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String remoteAddr = getClientIp(request);
        String endpoint = request.getRequestURI();

        try {
            int limitPerMinute = getLimitForEndpoint(endpoint);
            BucketProxy bucket = resolveBucket(remoteAddr, endpoint, limitPerMinute);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (!probe.isConsumed()) {
                long retryAfter = Math.max(1, (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0));
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(retryAfter));
                response.setHeader("X-Rate-Limit-Remaining", "0");
                response.getWriter().write("{\"status\": 429, \"code\": \"RATE_LIMIT_EXCEEDED\", \"message\": \"Too many requests. Retry after " + retryAfter + " seconds\"}");
                log.warn("Rate limit exceeded for {} on {}", remoteAddr, endpoint);
                return;
            }

            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
        } catch (Exception ex) {
            // Fail-open: Redis/Bucket4j issues should not block app availability.
            log.warn("Rate limiting skipped due to backend error on {} from {}", endpoint, remoteAddr, ex);
        }

        filterChain.doFilter(request, response);
    }

    private BucketProxy resolveBucket(String clientIp, String endpoint, int limitPerMinute) {
        String key = clientIp + ":" + endpoint;
        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limitPerMinute)
                        .refillGreedy(limitPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
        return proxyManager.builder().build(key.getBytes(StandardCharsets.UTF_8), configSupplier);
    }

    private int getLimitForEndpoint(String endpoint) {
        if (endpoint.contains("/auth/")) {
            return LOGIN_REQUESTS_PER_MINUTE;
        } else if (endpoint.contains("/transfer")) {
            return TRANSFER_REQUESTS_PER_MINUTE;
        }
        return REQUESTS_PER_MINUTE;
    }

    private String getClientIp(HttpServletRequest request) {
        if (trustProxy) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}

