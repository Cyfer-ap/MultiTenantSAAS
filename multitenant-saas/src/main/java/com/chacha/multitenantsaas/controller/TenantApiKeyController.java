package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TenantApiKeyCreateRequest;
import com.chacha.multitenantsaas.dto.TenantApiKeyCreatedResponse;
import com.chacha.multitenantsaas.dto.TenantApiKeyResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.service.TenantApiKeyService;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/api-keys")
public class TenantApiKeyController {

    private final TenantApiKeyService apiKeyService;
    private final CurrentActorService currentActorService;

    public TenantApiKeyController(
            TenantApiKeyService apiKeyService, CurrentActorService currentActorService) {
        this.apiKeyService = apiKeyService;
        this.currentActorService = currentActorService;
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @PostMapping
    public ResponseEntity<ApiResponse<TenantApiKeyCreatedResponse>> create(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantApiKeyCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        TenantApiKeyCreatedResponse response = apiKeyService.create(tenantId, actor, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "API key created. Store it now because it will not be shown again.",
                                response));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantApiKeyResponse>>> list(
            @PathVariable UUID tenantId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Tenant API keys fetched successfully",
                                apiKeyService.list(tenantId)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @DeleteMapping("/{apiKeyId}")
    public ResponseEntity<ApiResponse<TenantApiKeyResponse>> revoke(
            @PathVariable UUID tenantId,
            @PathVariable UUID apiKeyId,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        TenantApiKeyResponse response = apiKeyService.revoke(tenantId, apiKeyId, actor);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Tenant API key revoked successfully", response));
    }
}
