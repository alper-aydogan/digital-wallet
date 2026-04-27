package com.alper.digitalwallet.infrastructure.config;

/**
 * Public (permitAll) endpoint paths used across security components.
 * This ensures consistency between SecurityConfig and JwtAuthenticationFilter.
 */
public final class SecurityPaths {

    private SecurityPaths() {
        // Utility class
    }

    /**
     * Public paths that don't require authentication.
     * Used by both SecurityConfig (authorization) and JwtAuthenticationFilter (skip filtering).
     */
    public static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/actuator/health",
            "/h2-console/**",
            "/demo",
            "/demo/**",
            "/",
            "/index.html"
    };
}
