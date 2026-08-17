package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.LoginResponse;
import com.chacha.multitenantsaas.dto.VerifiedTenantLoginRequest;
import com.chacha.multitenantsaas.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Authentication", description = "Login API for tenant users")
@RequestMapping("/api/tenants/{tenantId}/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login tenant user",
            description = "Authenticates a tenant user and returns access and refresh tokens.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @PathVariable UUID tenantId, @Valid @RequestBody VerifiedTenantLoginRequest request) {
        LoginResponse response = authService.loginVerified(tenantId, request);

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
