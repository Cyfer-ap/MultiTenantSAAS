package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.SystemAdminLoginRequest;
import com.chacha.multitenantsaas.dto.SystemAdminLoginResponse;
import com.chacha.multitenantsaas.service.SystemAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
}