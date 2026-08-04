package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.AuthorizationProvisioningBackfillResponse;
import com.chacha.multitenantsaas.dto.AuthorizationProvisioningReadinessResponse;
import com.chacha.multitenantsaas.dto.AuthorizationProvisioningSummary;
import com.chacha.multitenantsaas.service.AuthorizationProvisioningService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/tenants/{tenantId}"
                + "/authorization/provisioning"
)
public class AuthorizationProvisioningController {

    private final AuthorizationProvisioningService
            authorizationProvisioningService;

    public AuthorizationProvisioningController(
            AuthorizationProvisioningService
                    authorizationProvisioningService
    ) {
        this.authorizationProvisioningService =
                authorizationProvisioningService;
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
                    + " or "
                    + "@systemSecurity.isSystemAdmin()"
    )
    @GetMapping("/readiness")
    public ResponseEntity<
            ApiResponse<
                    AuthorizationProvisioningReadinessResponse
                    >
            >
    getReadiness(
            @PathVariable UUID tenantId
    ) {
        AuthorizationProvisioningReadinessResponse
                readiness =
                authorizationProvisioningService
                        .getTenantReadiness(
                                tenantId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        readiness.ready()
                                ? "Authorization V2 tenant "
                                + "is ready"
                                : "Authorization V2 tenant "
                                + "requires backfill",
                        readiness
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
                    + " or "
                    + "@systemSecurity.isSystemAdmin()"
    )
    @PostMapping("/backfill")
    public ResponseEntity<
            ApiResponse<
                    AuthorizationProvisioningBackfillResponse
                    >
            >
    backfillTenant(
            @PathVariable UUID tenantId
    ) {
        AuthorizationProvisioningSummary summary =
                authorizationProvisioningService
                        .provisionTenantFromLegacyRoles(
                                tenantId
                        );

        AuthorizationProvisioningReadinessResponse
                readiness =
                authorizationProvisioningService
                        .getTenantReadiness(
                                tenantId
                        );

        AuthorizationProvisioningBackfillResponse
                response =
                new AuthorizationProvisioningBackfillResponse(
                        summary,
                        readiness
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        readiness.ready()
                                ? "Authorization V2 backfill "
                                + "completed successfully"
                                : "Authorization V2 backfill "
                                + "completed with unresolved "
                                + "issues",
                        response
                )
        );
    }
}