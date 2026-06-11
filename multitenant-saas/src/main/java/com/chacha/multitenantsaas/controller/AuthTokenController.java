package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.RefreshTokenRequest;
import com.chacha.multitenantsaas.dto.TokenRefreshResponse;
import com.chacha.multitenantsaas.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthTokenController {

    private final AuthService authService;

    public AuthTokenController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        TokenRefreshResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", response)
        );
    }
}

