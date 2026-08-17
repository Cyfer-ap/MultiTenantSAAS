package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.LoginResponse;
import com.chacha.multitenantsaas.dto.VerifiedTenantLoginRequest;
import com.chacha.multitenantsaas.service.AuthService;
import com.chacha.multitenantsaas.service.BrowserSessionCookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Authentication", description = "Login API for tenant users")
@RequestMapping("/api/tenants/{tenantId}/auth")
public class AuthController {

    private final AuthService authService;
    private final BrowserSessionCookieService browserSessionCookieService;

    public AuthController(
            AuthService authService, BrowserSessionCookieService browserSessionCookieService) {
        this.authService = authService;
        this.browserSessionCookieService = browserSessionCookieService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login tenant user",
            description =
                    "Authenticates a verified tenant user and creates a browser refresh session.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @PathVariable UUID tenantId,
            @Valid @RequestBody VerifiedTenantLoginRequest request,
            HttpServletResponse servletResponse) {
        LoginResponse response = authService.loginVerified(tenantId, request);
        LoginResponse clientResponse =
                browserSessionCookieService.applyLoginSession(servletResponse, response);

        return ResponseEntity.ok(ApiResponse.success("Login successful", clientResponse));
    }
}
