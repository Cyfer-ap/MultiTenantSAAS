package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.ChangePasswordRequest;
import com.chacha.multitenantsaas.dto.ChangePasswordResponse;
import com.chacha.multitenantsaas.dto.LogoutRequest;
import com.chacha.multitenantsaas.dto.LogoutResponse;
import com.chacha.multitenantsaas.dto.RefreshTokenRequest;
import com.chacha.multitenantsaas.dto.TokenRefreshResponse;
import com.chacha.multitenantsaas.service.AuthService;
import com.chacha.multitenantsaas.service.BrowserSessionCookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(
        name = "Token Management",
        description = "Refresh token, logout, logout-all, and password change APIs")
@RequestMapping("/api/auth")
public class AuthTokenController {

    private final AuthService authService;
    private final BrowserSessionCookieService browserSessionCookieService;

    public AuthTokenController(
            AuthService authService, BrowserSessionCookieService browserSessionCookieService) {
        this.authService = authService;
        this.browserSessionCookieService = browserSessionCookieService;
    }

    @Operation(
            summary = "Refresh access token",
            description = "Rotates the browser refresh credential and returns a new access token.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @Valid @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String rawRefreshToken =
                browserSessionCookieService.resolveRefreshToken(
                        servletRequest, request == null ? null : request.refreshToken());

        TokenRefreshResponse response =
                browserSessionCookieService.isCookieMode()
                        ? authService.refreshToken(
                                rawRefreshToken,
                                browserSessionCookieService.resolveCsrfToken(servletRequest))
                        : authService.refreshToken(new RefreshTokenRequest(rawRefreshToken));

        TokenRefreshResponse clientResponse =
                browserSessionCookieService.applyRefreshSession(servletResponse, response);

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", clientResponse));
    }

    @Operation(
            summary = "Logout user",
            description = "Logs out the current browser refresh session.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @Valid @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String rawRefreshToken =
                browserSessionCookieService.resolveRefreshToken(
                        servletRequest, request == null ? null : request.refreshToken());

        LogoutResponse response =
                browserSessionCookieService.isCookieMode()
                        ? authService.logout(
                                rawRefreshToken,
                                browserSessionCookieService.resolveCsrfToken(servletRequest))
                        : authService.logout(new LogoutRequest(rawRefreshToken));

        browserSessionCookieService.clearRefreshCookie(servletResponse);

        return ResponseEntity.ok(ApiResponse.success("Logout successful", response));
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Logout all devices",
            description = "Revokes all active refresh tokens for the authenticated user.")
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<LogoutResponse>> logoutAllDevices(
            @AuthenticationPrincipal Jwt jwt, HttpServletResponse servletResponse) {
        LogoutResponse response = authService.logoutAllDevices(jwt);
        browserSessionCookieService.clearRefreshCookie(servletResponse);

        return ResponseEntity.ok(
                ApiResponse.success("Logged out from all devices successfully", response));
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Change password",
            description =
                    "Changes the authenticated user's password and revokes all active refresh tokens.")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse servletResponse) {
        ChangePasswordResponse response = authService.changePassword(jwt, request);
        browserSessionCookieService.clearRefreshCookie(servletResponse);

        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", response));
    }
}
