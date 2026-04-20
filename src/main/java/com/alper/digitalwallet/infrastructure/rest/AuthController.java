package com.alper.digitalwallet.infrastructure.rest;

import com.alper.digitalwallet.infrastructure.security.JwtProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Profile("dev")
public class AuthController {

    private final JwtProvider jwtProvider;

    @PostMapping("/demo-token")
    public ResponseEntity<DemoTokenResponse> createDemoToken(@Valid @RequestBody DemoTokenRequest request) {
        String token = jwtProvider.generateToken(request.getUserId());
        return ResponseEntity.ok(new DemoTokenResponse(token));
    }

    @Getter
    @Setter
    public static class DemoTokenRequest {
        @NotNull(message = "Kullanici ID bos olamaz")
        private Long userId;
    }

    @Getter
    @AllArgsConstructor
    public static class DemoTokenResponse {
        private String token;
    }
}

