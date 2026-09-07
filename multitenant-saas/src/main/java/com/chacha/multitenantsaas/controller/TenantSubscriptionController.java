package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionAccessResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionEntitlementResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionHistoryResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionResponse;
import com.chacha.multitenantsaas.service.SubscriptionEntitlementService;
import com.chacha.multitenantsaas.service.TenantSubscriptionHistoryService;
import com.chacha.multitenantsaas.service.TenantSubscriptionService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/subscription")
public class TenantSubscriptionController {

    private final TenantSubscriptionService tenantSubscriptionService;

    private final SubscriptionEntitlementService subscriptionEntitlementService;

    private final TenantSubscriptionHistoryService historyService;

    public TenantSubscriptionController(
            TenantSubscriptionService tenantSubscriptionService,
            SubscriptionEntitlementService subscriptionEntitlementService,
            TenantSubscriptionHistoryService historyService) {
        this.tenantSubscriptionService = tenantSubscriptionService;
        this.subscriptionEntitlementService = subscriptionEntitlementService;
        this.historyService = historyService;
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'subscription.read'"
                    + ")"
                    + " or "
                    + "@systemSecurity.isSystemAdmin()")
    @GetMapping
    public ResponseEntity<ApiResponse<TenantSubscriptionResponse>> getSubscription(
            @PathVariable UUID tenantId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tenant subscription fetched successfully",
                        tenantSubscriptionService.getSubscription(tenantId)));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'subscription.read'"
                    + ")"
                    + " or "
                    + "@systemSecurity.isSystemAdmin()")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<TenantSubscriptionHistoryResponse>>> getHistory(
            @PathVariable UUID tenantId,
            @PageableDefault(size = 20, sort = "recordedAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tenant subscription history fetched successfully",
                        historyService.getHistory(tenantId, pageable)));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".isCurrentTenant(#tenantId)"
                    + " or "
                    + "@systemSecurity.isSystemAdmin()")
    @GetMapping("/access")
    public ResponseEntity<ApiResponse<TenantSubscriptionAccessResponse>> getAccess(
            @PathVariable UUID tenantId) {
        TenantSubscriptionEntitlementResponse entitlements =
                subscriptionEntitlementService.evaluate(tenantId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tenant subscription access " + "evaluated successfully",
                        TenantSubscriptionAccessResponse.from(entitlements)));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'subscription.read'"
                    + ")"
                    + " or "
                    + "@systemSecurity.isSystemAdmin()")
    @GetMapping("/entitlements")
    public ResponseEntity<ApiResponse<TenantSubscriptionEntitlementResponse>> getEntitlements(
            @PathVariable UUID tenantId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tenant subscription entitlements " + "evaluated successfully",
                        subscriptionEntitlementService.evaluate(tenantId)));
    }
}
