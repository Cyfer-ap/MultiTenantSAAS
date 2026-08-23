package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.billing.dto.BillingCheckoutConfigurationResponse;
import com.chacha.multitenantsaas.billing.dto.BillingCheckoutRequest;
import com.chacha.multitenantsaas.billing.dto.BillingCheckoutResponse;
import com.chacha.multitenantsaas.billing.provider.BillingCheckoutSession;
import com.chacha.multitenantsaas.billing.service.BillingCheckoutService;
import com.chacha.multitenantsaas.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/billing")
public class BillingCheckoutController {

    private final BillingCheckoutService billingCheckoutService;

    public BillingCheckoutController(BillingCheckoutService billingCheckoutService) {
        this.billingCheckoutService = billingCheckoutService;
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'tenant.update'"
                    + ")")
    @GetMapping("/checkout/configuration")
    public ResponseEntity<ApiResponse<BillingCheckoutConfigurationResponse>>
            getCheckoutConfiguration(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Billing checkout configuration fetched successfully",
                        billingCheckoutService.getCheckoutConfiguration()));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'tenant.update'"
                    + ")")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<BillingCheckoutResponse>> createCheckout(
            @PathVariable UUID tenantId, @Valid @RequestBody BillingCheckoutRequest request) {
        BillingCheckoutSession session =
                billingCheckoutService.createCheckoutSession(
                        tenantId, request.planCode(), request.provider());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Billing checkout session created successfully",
                        BillingCheckoutResponse.from(session)));
    }
}
