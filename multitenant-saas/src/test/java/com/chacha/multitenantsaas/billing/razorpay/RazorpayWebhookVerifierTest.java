package com.chacha.multitenantsaas.billing.razorpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import com.chacha.multitenantsaas.billing.webhook.WebhookSignatureSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class RazorpayWebhookVerifierTest {

    private static final String SECRET = "razorpay_webhook_secret";

    private RazorpayWebhookVerifier verifier;

    @BeforeEach
    void setUp() {
        RazorpayBillingProperties properties = new RazorpayBillingProperties();
        properties.setWebhookEnabled(true);
        properties.setWebhookSecret(SECRET);
        verifier = new RazorpayWebhookVerifier(properties, JsonMapper.builder().build());
    }

    @Test
    void verifiesRawPayloadAndUsesProviderEventHeaderForIdentity() {
        String payload = "{\"event\":\"subscription.activated\"}";
        String signature = WebhookSignatureSupport.signAsHex(SECRET, payload);

        VerifiedBillingEvent event = verifier.verify(payload, signature, "event_header_123");

        assertThat(event.provider()).isEqualTo(BillingProviderType.RAZORPAY);
        assertThat(event.providerEventId()).isEqualTo("event_header_123");
        assertThat(event.eventType()).isEqualTo("subscription.activated");
        assertThat(event.payload()).isEqualTo(payload);
    }

    @Test
    void rejectsSignatureForDifferentRawPayload() {
        String signedPayload = "{\"event\":\"subscription.activated\"}";
        String signature = WebhookSignatureSupport.signAsHex(SECRET, signedPayload);

        assertThatThrownBy(() -> verifier.verify(signedPayload + " ", signature, "event_123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Razorpay webhook signature");
    }

    @Test
    void requiresRazorpayEventIdForIdempotency() {
        String payload = "{\"event\":\"subscription.cancelled\"}";
        String signature = WebhookSignatureSupport.signAsHex(SECRET, payload);

        assertThatThrownBy(() -> verifier.verify(payload, signature, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing x-razorpay-event-id header");
    }
}
