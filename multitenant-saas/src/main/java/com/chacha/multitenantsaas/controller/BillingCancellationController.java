package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.billing.dto.BillingCancellationResponse;
import com.chacha.multitenantsaas.billing.provider.BillingCancellationResult;
import com.chacha.multitenantsaas.billing.service.BillingCancellationService;
import com.chacha.multitenantsaas.common.ApiResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/billing")
public class BillingCancellationController {

    private final BillingCancellationService billingCancellationService;

    public BillingCancellationController(BillingCancellationService billingCancellationService) {
        this.billingCancellationService = billingCancellationService;
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'tenant.update'"
                    + ")")
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<BillingCancellationResponse>> cancelSubscription(
            @PathVariable UUID tenantId) {
        BillingCancellationResult result = billingCancellationService.requestCancellation(tenantId);

        return ResponseEntity.accepted()
                .body(
                        ApiResponse.success(
                                "Billing cancellation requested; subscription state will update "
                                        + "after provider webhook reconciliation",
                                BillingCancellationResponse.from(result)));
    }
}
