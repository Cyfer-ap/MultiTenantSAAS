package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.OutboundWebhookDeliveryDetailResponse;
import com.chacha.multitenantsaas.dto.OutboundWebhookDeliveryResponse;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.OutboundWebhookDeliveryStatus;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.service.OutboundWebhookDeliveryManagementService;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/outbound-webhooks/deliveries")
public class OutboundWebhookDeliveryController {

    private final OutboundWebhookDeliveryManagementService managementService;
    private final CurrentActorService currentActorService;

    public OutboundWebhookDeliveryController(
            OutboundWebhookDeliveryManagementService managementService,
            CurrentActorService currentActorService) {
        this.managementService = managementService;
        this.currentActorService = currentActorService;
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OutboundWebhookDeliveryResponse>>> list(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) UUID endpointId,
            @RequestParam(required = false) OutboundWebhookDeliveryStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Outbound webhook deliveries fetched successfully",
                                managementService.list(tenantId, endpointId, status, pageable)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @GetMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<OutboundWebhookDeliveryDetailResponse>> get(
            @PathVariable UUID tenantId, @PathVariable UUID deliveryId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Outbound webhook delivery fetched successfully",
                                managementService.get(tenantId, deliveryId)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId, 'tenant.update')")
    @PostMapping("/{deliveryId}/replay")
    public ResponseEntity<ApiResponse<OutboundWebhookDeliveryResponse>> replay(
            @PathVariable UUID tenantId,
            @PathVariable UUID deliveryId,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Outbound webhook delivery queued for replay",
                                managementService.replay(tenantId, deliveryId, actor)));
    }
}
