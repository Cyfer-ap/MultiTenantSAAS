package com.chacha.multitenantsaas.billing.stripe;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import com.chacha.multitenantsaas.billing.webhook.WebhookSignatureSupport;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StripeWebhookVerifier {

    private final StripeBillingProperties properties;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public StripeWebhookVerifier(StripeBillingProperties properties, JsonMapper jsonMapper) {
        this(properties, jsonMapper, Clock.systemUTC());
    }

    StripeWebhookVerifier(StripeBillingProperties properties, JsonMapper jsonMapper, Clock clock) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    public VerifiedBillingEvent verify(String payload, String signatureHeader) {
        requireConfigured();
        StripeSignatures signatures = parseSignatures(signatureHeader);
        enforceTimestampTolerance(signatures.timestamp());

        String signedPayload = signatures.timestamp() + "." + payload;
        boolean valid =
                signatures.v1Signatures().stream()
                        .anyMatch(
                                signature ->
                                        WebhookSignatureSupport.matches(
                                                properties.getWebhookSecret(),
                                                signedPayload,
                                                signature));
        if (!valid) {
            throw new IllegalArgumentException("Invalid Stripe webhook signature");
        }

        JsonNode event = parsePayload(payload);
        return new VerifiedBillingEvent(
                BillingProviderType.STRIPE,
                requiredText(event, "id"),
                requiredText(event, "type"),
                payload);
    }

    private void requireConfigured() {
        if (!properties.isWebhookEnabled()) {
            throw new IllegalArgumentException("Stripe webhook ingestion is disabled");
        }
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            throw new IllegalStateException(
                    "STRIPE_WEBHOOK_SECRET must be configured when Stripe webhooks are enabled");
        }
    }

    private StripeSignatures parseSignatures(String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new IllegalArgumentException("Missing Stripe-Signature header");
        }

        Long timestamp = null;
        List<String> signatures = new ArrayList<>();
        for (String item : signatureHeader.split(",")) {
            String[] parts = item.trim().split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            if ("t".equals(parts[0])) {
                timestamp = parseTimestamp(parts[1]);
            } else if ("v1".equals(parts[0])) {
                signatures.add(parts[1]);
            }
        }

        if (timestamp == null || signatures.isEmpty()) {
            throw new IllegalArgumentException("Malformed Stripe-Signature header");
        }
        return new StripeSignatures(timestamp, signatures);
    }

    private long parseTimestamp(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Malformed Stripe-Signature timestamp", exception);
        }
    }

    private void enforceTimestampTolerance(long timestamp) {
        long ageSeconds = Math.abs(clock.instant().getEpochSecond() - timestamp);
        if (ageSeconds > properties.getWebhookToleranceSeconds()) {
            throw new IllegalArgumentException(
                    "Stripe webhook signature timestamp is outside tolerance");
        }
    }

    private JsonNode parsePayload(String payload) {
        try {
            return jsonMapper.readTree(payload);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Malformed Stripe webhook payload", exception);
        }
    }

    private String requiredText(JsonNode event, String fieldName) {
        JsonNode value = event.get(fieldName);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new IllegalArgumentException("Stripe webhook payload is missing " + fieldName);
        }
        return value.asString();
    }

    private record StripeSignatures(long timestamp, List<String> v1Signatures) {}
}
