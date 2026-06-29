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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(
        name = "Tenant Dashboard",
        description = "Tenant-level dashboard summary APIs"
)
@SecurityRequirement(name = "bearerAuth")
public class TenantDashboardController {

    private final TenantDashboardService tenantDashboardService;

    public TenantDashboardController(TenantDashboardService tenantDashboardService) {
        this.tenantDashboardService = tenantDashboardService;
    }

    @Operation(
            summary = "Get tenant dashboard summary",
            description = "Returns user counts for the authenticated user's tenant."
    )
    @PreAuthorize("@tenantSecurity.isCurrentTenantAdminOrManager()")
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