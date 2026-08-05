package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionLifecycleUpdateRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionPlanChangeRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionStartRequest;
import com.chacha.multitenantsaas.service.SubscriptionAdministrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/system/tenants/{tenantId}/subscription"
)
public class SystemTenantSubscriptionController {

    private final SubscriptionAdministrationService
            subscriptionAdministrationService;

    public SystemTenantSubscriptionController(
            SubscriptionAdministrationService
                    subscriptionAdministrationService
    ) {
        this.subscriptionAdministrationService =
                subscriptionAdministrationService;
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @GetMapping
    public ResponseEntity<
            ApiResponse<TenantSubscriptionResponse>
            >
    getSubscription(
            @PathVariable UUID tenantId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tenant subscription fetched successfully",
                        subscriptionAdministrationService
                                .getTenantSubscription(tenantId)
                )
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PostMapping
    public ResponseEntity<
            ApiResponse<TenantSubscriptionResponse>
            >
    startSubscription(
            @PathVariable UUID tenantId,
            @Valid @RequestBody
            TenantSubscriptionStartRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tenant subscription started successfully",
                        subscriptionAdministrationService
                                .startTenantSubscription(
                                        tenantId,
                                        request,
                                        jwt
                                )
                )
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PutMapping("/plan")
    public ResponseEntity<
            ApiResponse<TenantSubscriptionResponse>
            >
    changePlan(
            @PathVariable UUID tenantId,
            @Valid @RequestBody
            TenantSubscriptionPlanChangeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tenant subscription plan changed successfully",
                        subscriptionAdministrationService
                                .changeTenantPlan(
                                        tenantId,
                                        request,
                                        jwt
                                )
                )
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PatchMapping("/lifecycle")
    public ResponseEntity<
            ApiResponse<TenantSubscriptionResponse>
            >
    updateLifecycle(
            @PathVariable UUID tenantId,
            @Valid @RequestBody
            TenantSubscriptionLifecycleUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tenant subscription lifecycle updated successfully",
                        subscriptionAdministrationService
                                .updateTenantSubscriptionLifecycle(
                                        tenantId,
                                        request,
                                        jwt
                                )
                )
        );
    }
}
