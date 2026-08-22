package com.chacha.multitenantsaas.billing.razorpay;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import com.chacha.multitenantsaas.billing.webhook.WebhookSignatureSupport;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RazorpayWebhookVerifier {

    private final RazorpayBillingProperties properties;
    private final JsonMapper jsonMapper;

    public RazorpayWebhookVerifier(
            RazorpayBillingProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    public VerifiedBillingEvent verify(
            String payload, String signature, String providerEventId) {
        requireConfigured();
        if (!WebhookSignatureSupport.matches(
                properties.getWebhookSecret(), payload, signature)) {
            throw new IllegalArgumentException("Invalid Razorpay webhook signature");
        }
        if (providerEventId == null || providerEventId.isBlank()) {
            throw new IllegalArgumentException("Missing x-razorpay-event-id header");
        }

        JsonNode event = parsePayload(payload);
        JsonNode eventType = event.get("event");
        if (eventType == null || !eventType.isString() || eventType.asString().isBlank()) {
            throw new IllegalArgumentException("Razorpay webhook payload is missing event");
        }

        return new VerifiedBillingEvent(
                BillingProviderType.RAZORPAY,
                providerEventId.trim(),
                eventType.asString(),
                payload);
    }

    private void requireConfigured() {
        if (!properties.isWebhookEnabled()) {
            throw new IllegalArgumentException("Razorpay webhook ingestion is disabled");
        }
        if (properties.getWebhookSecret() == null
                || properties.getWebhookSecret().isBlank()) {
            throw new IllegalStateException(
                    "RAZORPAY_WEBHOOK_SECRET must be configured when Razorpay webhooks are enabled");
        }
    }

    private JsonNode parsePayload(String payload) {
        try {
            return jsonMapper.readTree(payload);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Malformed Razorpay webhook payload", exception);
        }
    }
}
