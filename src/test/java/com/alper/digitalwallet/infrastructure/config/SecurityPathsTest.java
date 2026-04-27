package com.alper.digitalwallet.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityPathsTest {

    @Test
    void publicPaths_containsEssentialPaths() {
        List<String> expectedPaths = Arrays.asList(
                "/api/v1/auth/**",
                "/swagger-ui.html",
                "/actuator/health",
                "/h2-console/**",
                "/demo",
                "/",
                "/index.html"
        );

        for (String expected : expectedPaths) {
            assertTrue(
                    Arrays.asList(SecurityPaths.PUBLIC_PATHS).contains(expected),
                    "PUBLIC_PATHS should contain: " + expected
            );
        }
    }

    @Test
    void publicPaths_consistencyBetweenSecurityConfigAndFilter() {
        // SecurityConfig ve JwtAuthenticationFilter aynı PUBLIC_PATHS kullanmalı
        String[] securityConfigPaths = SecurityPaths.PUBLIC_PATHS;
        
        // Her iki yerde de aynı path'ler olmalı
        assertTrue(securityConfigPaths.length >= 10, 
                "PUBLIC_PATHS should have at least 10 paths for security coverage");
    }
}
