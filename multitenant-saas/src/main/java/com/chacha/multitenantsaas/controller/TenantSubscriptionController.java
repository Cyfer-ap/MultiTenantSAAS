package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionResponse;
import com.chacha.multitenantsaas.service.TenantSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenants/{tenantId}/subscription")
public class TenantSubscriptionController {

    private final TenantSubscriptionService
            tenantSubscriptionService;

    public TenantSubscriptionController(
            TenantSubscriptionService tenantSubscriptionService
    ) {
        this.tenantSubscriptionService =
                tenantSubscriptionService;
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'subscription.read'"
                    + ")"
                    + " or "
                    + "@systemSecurity.isSystemAdmin()"
    )
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
                        tenantSubscriptionService
                                .getSubscription(tenantId)
                )
        );
    }
}
