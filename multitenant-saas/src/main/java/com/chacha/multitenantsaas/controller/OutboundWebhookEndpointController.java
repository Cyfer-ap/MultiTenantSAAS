package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.OutboundWebhookEndpointCreateRequest;
import com.chacha.multitenantsaas.dto.OutboundWebhookEndpointCreatedResponse;
import com.chacha.multitenantsaas.dto.OutboundWebhookEndpointResponse;
import com.chacha.multitenantsaas.dto.OutboundWebhookEndpointUpdateRequest;
import com.chacha.multitenantsaas.dto.OutboundWebhookSecretRotatedResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.service.OutboundWebhookEndpointService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/outbound-webhooks")
public class OutboundWebhookEndpointController {

    private final OutboundWebhookEndpointService endpointService;
    private final CurrentActorService currentActorService;

    public OutboundWebhookEndpointController(
            OutboundWebhookEndpointService endpointService,
            CurrentActorService currentActorService) {
        this.endpointService = endpointService;
        this.currentActorService = currentActorService;
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @PostMapping
    public ResponseEntity<ApiResponse<OutboundWebhookEndpointCreatedResponse>> create(
            @PathVariable UUID tenantId,
            @Valid @RequestBody OutboundWebhookEndpointCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        OutboundWebhookEndpointCreatedResponse response =
                endpointService.create(tenantId, actor, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Outbound webhook created. Store the signing secret now because it will not be shown again.",
                                response));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OutboundWebhookEndpointResponse>>> list(
            @PathVariable UUID tenantId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Outbound webhook endpoints fetched successfully",
                                endpointService.list(tenantId)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<String>>> eventCatalog(@PathVariable UUID tenantId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Outbound webhook event catalog fetched successfully",
                                endpointService.eventCatalog()));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @GetMapping("/{endpointId}")
    public ResponseEntity<ApiResponse<OutboundWebhookEndpointResponse>> get(
            @PathVariable UUID tenantId, @PathVariable UUID endpointId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Outbound webhook endpoint fetched successfully",
                                endpointService.get(tenantId, endpointId)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @PutMapping("/{endpointId}")
    public ResponseEntity<ApiResponse<OutboundWebhookEndpointResponse>> update(
            @PathVariable UUID tenantId,
            @PathVariable UUID endpointId,
            @Valid @RequestBody OutboundWebhookEndpointUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Outbound webhook endpoint updated successfully",
                                endpointService.update(tenantId, endpointId, actor, request)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @PostMapping("/{endpointId}/rotate-secret")
    public ResponseEntity<ApiResponse<OutboundWebhookSecretRotatedResponse>> rotateSecret(
            @PathVariable UUID tenantId,
            @PathVariable UUID endpointId,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Outbound webhook signing secret rotated. Store the new secret now because it will not be shown again.",
                                endpointService.rotateSecret(tenantId, endpointId, actor)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @DeleteMapping("/{endpointId}")
    public ResponseEntity<ApiResponse<OutboundWebhookEndpointResponse>> archive(
            @PathVariable UUID tenantId,
            @PathVariable UUID endpointId,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Outbound webhook endpoint archived successfully",
                                endpointService.archive(tenantId, endpointId, actor)));
    }
}
