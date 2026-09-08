package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TenantIdentityProviderCreateRequest;
import com.chacha.multitenantsaas.dto.TenantIdentityProviderResponse;
import com.chacha.multitenantsaas.dto.TenantIdentityProviderSecretRotateRequest;
import com.chacha.multitenantsaas.dto.TenantIdentityProviderSecretRotatedResponse;
import com.chacha.multitenantsaas.dto.TenantIdentityProviderUpdateRequest;
import com.chacha.multitenantsaas.dto.TenantIdentityProviderVerificationResponse;
import com.chacha.multitenantsaas.dto.TenantSsoPolicyUpdateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.service.TenantIdentityProviderService;
import com.chacha.multitenantsaas.service.TenantIdentityProviderVerificationManagementService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/identity-provider")
public class TenantIdentityProviderController {

    private final TenantIdentityProviderService identityProviderService;
    private final TenantIdentityProviderVerificationManagementService verificationManagementService;
    private final CurrentActorService currentActorService;

    public TenantIdentityProviderController(
            TenantIdentityProviderService identityProviderService,
            TenantIdentityProviderVerificationManagementService verificationManagementService,
            CurrentActorService currentActorService) {
        this.identityProviderService = identityProviderService;
        this.verificationManagementService = verificationManagementService;
        this.currentActorService = currentActorService;
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @PostMapping
    public ResponseEntity<ApiResponse<TenantIdentityProviderResponse>> create(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantIdentityProviderCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Tenant identity-provider configuration created in draft state",
                                identityProviderService.create(tenantId, actor, request)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @GetMapping
    public ResponseEntity<ApiResponse<TenantIdentityProviderResponse>> get(
            @PathVariable UUID tenantId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Tenant identity-provider configuration fetched successfully",
                                identityProviderService.get(tenantId)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @PutMapping
    public ResponseEntity<ApiResponse<TenantIdentityProviderResponse>> update(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantIdentityProviderUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Tenant identity-provider configuration updated successfully",
                                identityProviderService.update(tenantId, actor, request)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @PutMapping("/policy")
    public ResponseEntity<ApiResponse<TenantIdentityProviderResponse>> updateSsoPolicy(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantSsoPolicyUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Tenant SSO policy updated successfully",
                                identityProviderService.updateSsoPolicy(tenantId, actor, request)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<TenantIdentityProviderVerificationResponse>> verify(
            @PathVariable UUID tenantId, @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Tenant identity-provider configuration verified successfully",
                                verificationManagementService.verify(tenantId, actor)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @PostMapping("/rotate-client-secret")
    public ResponseEntity<ApiResponse<TenantIdentityProviderSecretRotatedResponse>>
            rotateClientSecret(
                    @PathVariable UUID tenantId,
                    @Valid @RequestBody TenantIdentityProviderSecretRotateRequest request,
                    @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Tenant identity-provider client secret rotated successfully",
                                identityProviderService.rotateClientSecret(
                                        tenantId, actor, request.clientSecret())));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @PostMapping("/enable")
    public ResponseEntity<ApiResponse<TenantIdentityProviderResponse>> enable(
            @PathVariable UUID tenantId, @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Tenant identity-provider configuration enabled in draft state",
                                identityProviderService.enable(tenantId, actor)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @DeleteMapping
    public ResponseEntity<ApiResponse<TenantIdentityProviderResponse>> disable(
            @PathVariable UUID tenantId, @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Tenant identity-provider configuration disabled successfully",
                                identityProviderService.disable(tenantId, actor)));
    }
}
