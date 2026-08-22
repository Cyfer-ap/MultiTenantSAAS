package com.chacha.multitenantsaas.billing.stripe;

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
public class StripeSubscriptionEventMapper implements BillingSubscriptionEventMapper {

    private static final Set<String> SUPPORTED_EVENTS =
            Set.of(
                    "customer.subscription.created",
                    "customer.subscription.updated",
                    "customer.subscription.deleted");

    private final JsonMapper jsonMapper;

    public StripeSubscriptionEventMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public BillingProviderType providerType() {
        return BillingProviderType.STRIPE;
    }

    @Override
    public Optional<BillingSubscriptionUpdate> map(VerifiedBillingEvent event) {
        if (!SUPPORTED_EVENTS.contains(event.eventType())) {
            return Optional.empty();
        }

        JsonNode root = parse(event.payload());
        JsonNode subscription = requiredObject(requiredObject(root, "data"), "object");
        JsonNode metadata = requiredObject(subscription, "metadata");
        JsonNode periodSource = periodSource(subscription);
        TenantSubscriptionStatus status =
                "customer.subscription.deleted".equals(event.eventType())
                        ? TenantSubscriptionStatus.CANCELLED
                        : mapStatus(requiredText(subscription, "status"));
        Instant periodStart = requiredInstant(periodSource, "current_period_start");

        return Optional.of(
                new BillingSubscriptionUpdate(
                        BillingProviderType.STRIPE,
                        requiredText(subscription, "id"),
                        UUID.fromString(requiredText(metadata, "tenant_id")),
                        requiredText(metadata, "plan_code"),
                        status,
                        optionalInstant(subscription, "start_date").orElse(periodStart),
                        periodStart,
                        requiredInstant(periodSource, "current_period_end"),
                        optionalInstant(subscription, "trial_end").orElse(null),
                        optionalBoolean(subscription, "cancel_at_period_end"),
                        requiredInstant(root, "created")));
    }

    private JsonNode periodSource(JsonNode subscription) {
        if (hasIntegralNumber(subscription, "current_period_start")
                && hasIntegralNumber(subscription, "current_period_end")) {
            return subscription;
        }

        JsonNode items = requiredObject(subscription, "items");
        JsonNode data = items.get("data");
        if (data == null || !data.isArray() || data.size() == 0) {
            throw new IllegalArgumentException("Stripe subscription payload is missing items.data");
        }
        return data.get(0);
    }

    private TenantSubscriptionStatus mapStatus(String status) {
        return switch (status) {
            case "trialing" -> TenantSubscriptionStatus.TRIALING;
            case "active" -> TenantSubscriptionStatus.ACTIVE;
            case "past_due", "unpaid", "incomplete", "paused" -> TenantSubscriptionStatus.PAST_DUE;
            case "canceled" -> TenantSubscriptionStatus.CANCELLED;
            case "incomplete_expired" -> TenantSubscriptionStatus.EXPIRED;
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported Stripe subscription status: " + status);
        };
    }

    private JsonNode parse(String payload) {
        try {
            return jsonMapper.readTree(payload);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Malformed Stripe subscription payload", exception);
        }
    }

    private JsonNode requiredObject(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isObject()) {
            throw new IllegalArgumentException("Stripe subscription payload is missing " + field);
        }
        return value;
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new IllegalArgumentException("Stripe subscription payload is missing " + field);
        }
        return value.asString();
    }

    private Instant requiredInstant(JsonNode node, String field) {
        return optionalInstant(node, field)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Stripe subscription payload is missing " + field));
    }

    private Optional<Instant> optionalInstant(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        if (!value.isIntegralNumber()) {
            throw new IllegalArgumentException("Stripe subscription payload has invalid " + field);
        }
        return Optional.of(Instant.ofEpochSecond(value.asLong()));
    }

    private boolean hasIntegralNumber(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isIntegralNumber();
    }

    private boolean optionalBoolean(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isBoolean() && value.asBoolean();
    }
}
