package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.ChangePasswordRequest;
import com.chacha.multitenantsaas.dto.SystemAdminCurrentResponse;
import com.chacha.multitenantsaas.dto.SystemAdminLoginRequest;
import com.chacha.multitenantsaas.dto.SystemAdminLoginResponse;
import com.chacha.multitenantsaas.service.SystemAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/auth")
public class SystemAuthController {

    private final SystemAuthService systemAuthService;

    public SystemAuthController(SystemAuthService systemAuthService) {
        this.systemAuthService = systemAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SystemAdminLoginResponse>> login(
            @Valid @RequestBody SystemAdminLoginRequest request
    ) {
        SystemAdminLoginResponse response = systemAuthService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success("System admin login successful", response)
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SystemAdminCurrentResponse>> getCurrentSystemAdmin(
            @AuthenticationPrincipal Jwt jwt
    ) {
        SystemAdminCurrentResponse response = systemAuthService.getCurrentSystemAdmin(jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Current system admin fetched successfully", response)
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<SystemAdminCurrentResponse>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        SystemAdminCurrentResponse response = systemAuthService.changePassword(jwt, request);

        return ResponseEntity.ok(
                ApiResponse.success("System admin password changed successfully", response)
        );
    }
}