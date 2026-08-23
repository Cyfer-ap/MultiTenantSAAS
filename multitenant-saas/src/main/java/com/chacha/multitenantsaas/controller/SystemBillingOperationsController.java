package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.billing.dto.BillingEventOperationsResponse;
import com.chacha.multitenantsaas.billing.dto.BillingReconciliationResponse;
import com.chacha.multitenantsaas.billing.dto.BillingSubscriptionOperationsResponse;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.service.BillingOperationsService;
import com.chacha.multitenantsaas.billing.service.BillingReconciliationService;
import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.common.SortingUtils;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/billing")
public class SystemBillingOperationsController {

    private final BillingOperationsService billingOperationsService;
    private final BillingReconciliationService billingReconciliationService;

    public SystemBillingOperationsController(
            BillingOperationsService billingOperationsService,
            BillingReconciliationService billingReconciliationService) {
        this.billingOperationsService = billingOperationsService;
        this.billingReconciliationService = billingReconciliationService;
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @GetMapping("/subscriptions")
    public ResponseEntity<ApiResponse<PageResponse<BillingSubscriptionOperationsResponse>>>
            getLinkedSubscriptions(
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "10") int size,
                    @RequestParam(defaultValue = "updatedAt") String sortBy,
                    @RequestParam(defaultValue = "desc") String sortDir,
                    @RequestParam(required = false) BillingProviderType provider,
                    @RequestParam(required = false) TenantSubscriptionStatus status,
                    @RequestParam(required = false) String search) {
        Pageable pageable =
                PageRequest.of(
                        PaginationUtils.validatePage(page),
                        PaginationUtils.validateSize(size),
                        SortingUtils.getDirection(sortDir),
                        SortingUtils.validateSortBy(
                                sortBy,
                                "updatedAt",
                                "updatedAt",
                                "providerEventCreatedAt",
                                "currentPeriodEnd",
                                "status",
                                "billingProvider"));

        PageResponse<BillingSubscriptionOperationsResponse> response =
                billingOperationsService.getLinkedSubscriptions(provider, status, search, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Linked billing subscriptions fetched successfully", response));
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<PageResponse<BillingEventOperationsResponse>>>
            getBillingEvents(
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "10") int size,
                    @RequestParam(defaultValue = "receivedAt") String sortBy,
                    @RequestParam(defaultValue = "desc") String sortDir,
                    @RequestParam(required = false) BillingProviderType provider,
                    @RequestParam(required = false) String eventType,
                    @RequestParam(required = false) String search) {
        Pageable pageable =
                PageRequest.of(
                        PaginationUtils.validatePage(page),
                        PaginationUtils.validateSize(size),
                        SortingUtils.getDirection(sortDir),
                        SortingUtils.validateSortBy(
                                sortBy, "receivedAt", "receivedAt", "eventType", "provider"));

        PageResponse<BillingEventOperationsResponse> response =
                billingOperationsService.getBillingEvents(provider, eventType, search, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Billing events fetched successfully", response));
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PostMapping("/subscriptions/{tenantId}/reconcile")
    public ResponseEntity<ApiResponse<BillingReconciliationResponse>> reconcileSubscription(
            @PathVariable UUID tenantId) {
        BillingReconciliationResponse response =
                billingReconciliationService.reconcile(tenantId);

        return ResponseEntity.ok(
                ApiResponse.success("Billing subscription reconciliation completed", response));
    }
}
