package com.chacha.multitenantsaas.billing.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import com.chacha.multitenantsaas.billing.webhook.WebhookSignatureSupport;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class StripeWebhookVerifierTest {

    private static final String SECRET = "whsec_test";
    private static final long NOW = 1_750_000_000L;

    private StripeWebhookVerifier verifier;

    @BeforeEach
    void setUp() {
        StripeBillingProperties properties = new StripeBillingProperties();
        properties.setWebhookEnabled(true);
        properties.setWebhookSecret(SECRET);
        verifier =
                new StripeWebhookVerifier(
                        properties,
                        JsonMapper.builder().build(),
                        Clock.fixed(Instant.ofEpochSecond(NOW), ZoneOffset.UTC));
    }

    @Test
    void verifiesRawPayloadAndExtractsEventIdentity() {
        String payload = "{\"id\":\"evt_123\",\"type\":\"customer.subscription.updated\"}";
        String signature = signatureHeader(NOW, payload);

        VerifiedBillingEvent event = verifier.verify(payload, signature);

        assertThat(event.provider()).isEqualTo(BillingProviderType.STRIPE);
        assertThat(event.providerEventId()).isEqualTo("evt_123");
        assertThat(event.eventType()).isEqualTo("customer.subscription.updated");
        assertThat(event.payload()).isEqualTo(payload);
    }

    @Test
    void rejectsAValidSignatureOutsideReplayTolerance() {
        String payload = "{\"id\":\"evt_old\",\"type\":\"invoice.paid\"}";
        long staleTimestamp = NOW - 301;

        assertThatThrownBy(() -> verifier.verify(payload, signatureHeader(staleTimestamp, payload)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside tolerance");
    }

    @Test
    void rejectsSignatureForDifferentRawPayload() {
        String signedPayload = "{\"id\":\"evt_123\",\"type\":\"invoice.paid\"}";
        String receivedPayload = signedPayload + " ";

        assertThatThrownBy(
                        () -> verifier.verify(receivedPayload, signatureHeader(NOW, signedPayload)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Stripe webhook signature");
    }

    private String signatureHeader(long timestamp, String payload) {
        String signature = WebhookSignatureSupport.signAsHex(SECRET, timestamp + "." + payload);
        return "t=" + timestamp + ",v1=" + signature;
    }
}
