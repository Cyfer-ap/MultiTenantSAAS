package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/onboarding/tenants")
public class SystemTenantOnboardingController {

    private final TenantOnboardingService tenantOnboardingService;

    public SystemTenantOnboardingController(TenantOnboardingService tenantOnboardingService) {
        this.tenantOnboardingService = tenantOnboardingService;
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PostMapping
    public ResponseEntity<ApiResponse<TenantOnboardingResponse>> onboardTenantBySystemAdmin(
            @Valid @RequestBody TenantOnboardingRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        TenantOnboardingResponse response =
                tenantOnboardingService.onboardTenantBySystemAdmin(request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant onboarded successfully by system admin", response)
        );
    }
}