package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.AppUserCreateRequest;
import com.chacha.multitenantsaas.dto.AppUserResponse;
import com.chacha.multitenantsaas.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants/{tenantId}/users")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppUserResponse>> createUser(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AppUserCreateRequest request
    ) {
        AppUserResponse user = appUserService.createUser(tenantId, request);

        return ResponseEntity.ok(
                ApiResponse.success("User created successfully", user)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppUserResponse>>> getUsersByTenant(
            @PathVariable UUID tenantId
    ) {
        List<AppUserResponse> users = appUserService.getUsersByTenant(tenantId);

        return ResponseEntity.ok(
                ApiResponse.success("Users fetched successfully", users)
        );
    }
}