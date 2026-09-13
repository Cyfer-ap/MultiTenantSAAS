package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.AuthorizationDelegationCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationDelegationReferenceDataResponse;
import com.chacha.multitenantsaas.dto.AuthorizationDelegationResponse;
import com.chacha.multitenantsaas.service.AuthorizationDelegationCommandService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/authorization/delegations")
public class AuthorizationDelegationController {

    private final AuthorizationDelegationCommandService authorizationDelegationCommandService;

    public AuthorizationDelegationController(
            AuthorizationDelegationCommandService authorizationDelegationCommandService) {
        this.authorizationDelegationCommandService = authorizationDelegationCommandService;
    }

    @PreAuthorize(
            "@authorizationSecurity.hasTenantPermission(#tenantId,'authorization.delegate')"
                    + " or @authorizationSecurity.hasTenantPermission("
                    + "#tenantId,'authorization.manage')")
    @PostMapping
    public ResponseEntity<ApiResponse<AuthorizationDelegationResponse>> createDelegation(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AuthorizationDelegationCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AuthorizationDelegationResponse response =
                authorizationDelegationCommandService.createDelegation(tenantId, request, jwt);
        return ResponseEntity.ok(
                ApiResponse.success("Authorization delegated successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.hasTenantPermission(#tenantId,'authorization.delegate')"
                    + " or @authorizationSecurity.hasTenantPermission("
                    + "#tenantId,'authorization.manage')")
    @GetMapping("/reference-data")
    public ResponseEntity<ApiResponse<AuthorizationDelegationReferenceDataResponse>>
            getDelegationReferenceData(
                    @PathVariable UUID tenantId, @AuthenticationPrincipal Jwt jwt) {
        AuthorizationDelegationReferenceDataResponse response =
                authorizationDelegationCommandService.getReferenceData(tenantId, jwt);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization delegation reference data fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.hasTenantPermission(#tenantId,'authorization.delegate')"
                    + " or @authorizationSecurity.hasTenantPermission("
                    + "#tenantId,'authorization.manage')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AuthorizationDelegationResponse>>> getDelegations(
            @PathVariable UUID tenantId, @AuthenticationPrincipal Jwt jwt) {
        List<AuthorizationDelegationResponse> response =
                authorizationDelegationCommandService.getVisibleDelegations(tenantId, jwt);
        return ResponseEntity.ok(
                ApiResponse.success("Authorization delegations fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.hasTenantPermission(#tenantId,'authorization.delegate')"
                    + " or @authorizationSecurity.hasTenantPermission("
                    + "#tenantId,'authorization.manage')")
    @PatchMapping("/{delegationId}/revoke")
    public ResponseEntity<ApiResponse<AuthorizationDelegationResponse>> revokeDelegation(
            @PathVariable UUID tenantId,
            @PathVariable UUID delegationId,
            @AuthenticationPrincipal Jwt jwt) {
        AuthorizationDelegationResponse response =
                authorizationDelegationCommandService.revokeDelegation(tenantId, delegationId, jwt);
        return ResponseEntity.ok(
                ApiResponse.success("Authorization delegation revoked successfully", response));
    }
}
