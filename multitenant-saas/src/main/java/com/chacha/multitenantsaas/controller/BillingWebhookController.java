package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.billing.dto.BillingWebhookReceipt;
import com.chacha.multitenantsaas.billing.razorpay.RazorpayWebhookVerifier;
import com.chacha.multitenantsaas.billing.service.BillingWebhookService;
import com.chacha.multitenantsaas.billing.stripe.StripeWebhookVerifier;
import com.chacha.multitenantsaas.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/webhooks")
public class BillingWebhookController {

    private final StripeWebhookVerifier stripeWebhookVerifier;
    private final RazorpayWebhookVerifier razorpayWebhookVerifier;
    private final BillingWebhookService billingWebhookService;

    public BillingWebhookController(
            StripeWebhookVerifier stripeWebhookVerifier,
            RazorpayWebhookVerifier razorpayWebhookVerifier,
            BillingWebhookService billingWebhookService) {
        this.stripeWebhookVerifier = stripeWebhookVerifier;
        this.razorpayWebhookVerifier = razorpayWebhookVerifier;
        this.billingWebhookService = billingWebhookService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<ApiResponse<BillingWebhookReceipt>> receiveStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        BillingWebhookReceipt receipt =
                billingWebhookService.ingest(stripeWebhookVerifier.verify(payload, signature));
        return response(receipt);
    }

    @PostMapping("/razorpay")
    public ResponseEntity<ApiResponse<BillingWebhookReceipt>> receiveRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestHeader("x-razorpay-event-id") String providerEventId) {
        BillingWebhookReceipt receipt =
                billingWebhookService.ingest(
                        razorpayWebhookVerifier.verify(
                                payload, signature, providerEventId));
        return response(receipt);
    }

    private ResponseEntity<ApiResponse<BillingWebhookReceipt>> response(
            BillingWebhookReceipt receipt) {
        String message =
                receipt.duplicate()
                        ? "Billing webhook already received"
                        : "Billing webhook accepted";
        return ResponseEntity.ok(ApiResponse.success(message, receipt));
    }
}
