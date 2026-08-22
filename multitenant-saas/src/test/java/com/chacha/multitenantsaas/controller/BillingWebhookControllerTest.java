package com.chacha.multitenantsaas.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.dto.BillingWebhookReceipt;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.razorpay.RazorpayWebhookVerifier;
import com.chacha.multitenantsaas.billing.service.BillingWebhookService;
import com.chacha.multitenantsaas.billing.stripe.StripeWebhookVerifier;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import com.chacha.multitenantsaas.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class BillingWebhookControllerTest {

    @Test
    void verifiesAndPersistsStripeWebhook() {
        StripeWebhookVerifier stripeVerifier = mock(StripeWebhookVerifier.class);
        RazorpayWebhookVerifier razorpayVerifier = mock(RazorpayWebhookVerifier.class);
        BillingWebhookService service = mock(BillingWebhookService.class);
        BillingWebhookController controller =
                new BillingWebhookController(stripeVerifier, razorpayVerifier, service);
        VerifiedBillingEvent event =
                new VerifiedBillingEvent(
                        BillingProviderType.STRIPE,
                        "evt_123",
                        "invoice.paid",
                        "payload");
        BillingWebhookReceipt receipt =
                new BillingWebhookReceipt(
                        BillingProviderType.STRIPE,
                        "evt_123",
                        "invoice.paid",
                        false);
        when(stripeVerifier.verify("payload", "signature")).thenReturn(event);
        when(service.ingest(event)).thenReturn(receipt);

        ResponseEntity<ApiResponse<BillingWebhookReceipt>> response =
                controller.receiveStripeWebhook("payload", "signature");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).isEqualTo(receipt);
        verify(stripeVerifier).verify("payload", "signature");
        verify(service).ingest(event);
    }

    @Test
    void acknowledgesRazorpayDuplicate() {
        StripeWebhookVerifier stripeVerifier = mock(StripeWebhookVerifier.class);
        RazorpayWebhookVerifier razorpayVerifier = mock(RazorpayWebhookVerifier.class);
        BillingWebhookService service = mock(BillingWebhookService.class);
        BillingWebhookController controller =
                new BillingWebhookController(stripeVerifier, razorpayVerifier, service);
        VerifiedBillingEvent event =
                new VerifiedBillingEvent(
                        BillingProviderType.RAZORPAY,
                        "event_123",
                        "subscription.activated",
                        "payload");
        BillingWebhookReceipt receipt =
                new BillingWebhookReceipt(
                        BillingProviderType.RAZORPAY,
                        "event_123",
                        "subscription.activated",
                        true);
        when(razorpayVerifier.verify("payload", "signature", "event_123"))
                .thenReturn(event);
        when(service.ingest(event)).thenReturn(receipt);

        ResponseEntity<ApiResponse<BillingWebhookReceipt>> response =
                controller.receiveRazorpayWebhook(
                        "payload", "signature", "event_123");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Billing webhook already received");
        assertThat(response.getBody().data().duplicate()).isTrue();
    }
}
