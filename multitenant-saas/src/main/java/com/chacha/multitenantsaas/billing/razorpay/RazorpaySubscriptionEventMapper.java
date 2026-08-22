package com.chacha.multitenantsaas.billing.razorpay;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionEventMapper;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionUpdate;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RazorpaySubscriptionEventMapper implements BillingSubscriptionEventMapper {

    private static final Set<String> SUPPORTED_EVENTS =
            Set.of(
                    "subscription.activated",
                    "subscription.charged",
                    "subscription.updated",
                    "subscription.pending",
                    "subscription.halted",
                    "subscription.cancelled",
                    "subscription.completed");

    private final JsonMapper jsonMapper;

    public RazorpaySubscriptionEventMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public BillingProviderType providerType() {
        return BillingProviderType.RAZORPAY;
    }

    @Override
    public Optional<BillingSubscriptionUpdate> map(VerifiedBillingEvent event) {
        if (!SUPPORTED_EVENTS.contains(event.eventType())) {
            return Optional.empty();
        }

        JsonNode root = parse(event.payload());
        JsonNode payload = requiredObject(root, "payload");
        JsonNode subscription = requiredObject(requiredObject(payload, "subscription"), "entity");
        JsonNode notes = requiredObject(subscription, "notes");
        Instant periodStart = requiredInstant(subscription, "current_start");

        return Optional.of(
                new BillingSubscriptionUpdate(
                        BillingProviderType.RAZORPAY,
                        requiredText(subscription, "id"),
                        UUID.fromString(requiredText(notes, "tenant_id")),
                        requiredText(notes, "plan_code"),
                        mapStatus(event.eventType(), requiredText(subscription, "status")),
                        optionalInstant(subscription, "start_at").orElse(periodStart),
                        periodStart,
                        requiredInstant(subscription, "current_end"),
                        null,
                        false,
                        requiredInstant(root, "created_at")));
    }

    private TenantSubscriptionStatus mapStatus(String eventType, String status) {
        return switch (eventType) {
            case "subscription.cancelled" -> TenantSubscriptionStatus.CANCELLED;
            case "subscription.completed" -> TenantSubscriptionStatus.EXPIRED;
            case "subscription.pending", "subscription.halted" -> TenantSubscriptionStatus.PAST_DUE;
            default ->
                    switch (status) {
                        case "active" -> TenantSubscriptionStatus.ACTIVE;
                        case "pending", "halted" -> TenantSubscriptionStatus.PAST_DUE;
                        case "cancelled" -> TenantSubscriptionStatus.CANCELLED;
                        case "completed", "expired" -> TenantSubscriptionStatus.EXPIRED;
                        default ->
                                throw new IllegalArgumentException(
                                        "Unsupported Razorpay subscription status: " + status);
                    };
        };
    }

    private JsonNode parse(String payload) {
        try {
            return jsonMapper.readTree(payload);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Malformed Razorpay subscription payload", exception);
        }
    }

    private JsonNode requiredObject(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isObject()) {
            throw new IllegalArgumentException("Razorpay subscription payload is missing " + field);
        }
        return value;
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new IllegalArgumentException("Razorpay subscription payload is missing " + field);
        }
        return value.asString();
    }

    private Instant requiredInstant(JsonNode node, String field) {
        return optionalInstant(node, field)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Razorpay subscription payload is missing " + field));
    }

    private Optional<Instant> optionalInstant(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        if (!value.isIntegralNumber()) {
            throw new IllegalArgumentException(
                    "Razorpay subscription payload has invalid " + field);
        }
        return Optional.of(Instant.ofEpochSecond(value.asLong()));
    }
}
