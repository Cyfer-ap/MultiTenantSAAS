package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding/tenants")
public class TenantOnboardingController {

    private final TenantOnboardingService tenantOnboardingService;

    public TenantOnboardingController(TenantOnboardingService tenantOnboardingService) {
        this.tenantOnboardingService = tenantOnboardingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TenantOnboardingResponse>> onboardTenant(
            @Valid @RequestBody TenantOnboardingRequest request
    ) {
        TenantOnboardingResponse response = tenantOnboardingService.onboardTenant(request);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant onboarded successfully", response)
        );
    }
}
