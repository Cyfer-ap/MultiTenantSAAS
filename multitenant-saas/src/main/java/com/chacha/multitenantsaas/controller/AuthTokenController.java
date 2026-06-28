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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@Tag(
        name = "Token Management",
        description = "Refresh token, logout, logout-all, and password change APIs"
)
@RequestMapping("/api/auth")
public class AuthTokenController {

    private final AuthService authService;

    public AuthTokenController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Refresh access token",
            description = "Rotates the refresh token and returns a new access token and refresh token."
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        TokenRefreshResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", response)
        );
    }

    @Operation(
            summary = "Logout user",
            description = "Logs out the user by invalidating the refresh token."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        LogoutResponse response = authService.logout(request);

        return ResponseEntity.ok(
                ApiResponse.success("Logout successful", response)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Logout all devices",
            description = "Revokes all active refresh tokens for the authenticated user."
    )
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<LogoutResponse>> logoutAllDevices(
            @AuthenticationPrincipal Jwt jwt
    ) {
        LogoutResponse response = authService.logoutAllDevices(jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Logged out from all devices successfully", response)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Change password",
            description = "Changes the authenticated user's password and revokes all active refresh tokens."
    )
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

