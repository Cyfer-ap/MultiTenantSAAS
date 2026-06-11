package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TenantDashboardSummaryResponse;
import com.chacha.multitenantsaas.service.TenantDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantDashboardController {

    private final TenantDashboardService tenantDashboardService;

    public TenantDashboardController(TenantDashboardService tenantDashboardService) {
        this.tenantDashboardService = tenantDashboardService;
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'TENANT_MANAGER')")
    @GetMapping("/api/tenant/dashboard/summary")
    public ResponseEntity<ApiResponse<TenantDashboardSummaryResponse>> getTenantDashboardSummary(
            @AuthenticationPrincipal Jwt jwt
    ) {
        TenantDashboardSummaryResponse summary = tenantDashboardService.getTenantSummary(jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant dashboard summary fetched successfully", summary)
        );
    }
}