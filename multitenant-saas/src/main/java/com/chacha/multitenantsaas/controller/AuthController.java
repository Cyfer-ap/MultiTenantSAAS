package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.LoginRequest;
import com.chacha.multitenantsaas.dto.LoginResponse;
import com.chacha.multitenantsaas.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenants/{tenantId}/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @PathVariable UUID tenantId,
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(tenantId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", response)
        );
    }
}