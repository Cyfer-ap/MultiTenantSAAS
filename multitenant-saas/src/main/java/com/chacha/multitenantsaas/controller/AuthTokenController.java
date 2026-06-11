package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.RefreshTokenRequest;
import com.chacha.multitenantsaas.dto.TokenRefreshResponse;
import com.chacha.multitenantsaas.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.chacha.multitenantsaas.dto.LogoutRequest;
import com.chacha.multitenantsaas.dto.LogoutResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.chacha.multitenantsaas.dto.ChangePasswordRequest;
import com.chacha.multitenantsaas.dto.ChangePasswordResponse;


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

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        LogoutResponse response = authService.logout(request);

        return ResponseEntity.ok(
                ApiResponse.success("Logout successful", response)
        );
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<LogoutResponse>> logoutAllDevices(
            @AuthenticationPrincipal Jwt jwt
    ) {
        LogoutResponse response = authService.logoutAllDevices(jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Logged out from all devices successfully", response)
        );
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        ChangePasswordResponse response = authService.changePassword(jwt, request);

        return ResponseEntity.ok(
                ApiResponse.success("Password changed successfully", response)
        );
    }
}

