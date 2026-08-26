package com.chacha.multitenantsaas.billing.stripe;

import com.chacha.multitenantsaas.billing.provider.BillingCheckoutSession;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.billing.provider.BillingProviderSubscriptionSnapshot;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
@ConditionalOnProperty(prefix = "app.billing.stripe", name = "enabled", havingValue = "true")
public class StripeBillingProvider implements BillingProvider {

    private final StripeBillingProperties properties;
    private final RestClient restClient;

    @Autowired
    public StripeBillingProvider(StripeBillingProperties properties) {
        this(properties, RestClient.builder());
    }

    StripeBillingProvider(StripeBillingProperties properties, RestClient.Builder builder) {
        validateConfiguration(properties);
        this.properties = properties;
        this.restClient =
                builder.baseUrl(properties.getBaseUrl())
                        .defaultHeader(
                                HttpHeaders.AUTHORIZATION, "Bearer " + properties.getSecretKey())
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .build();
    }

    @Override
    public BillingProviderType providerType() {
        return BillingProviderType.STRIPE;
    }

    @Override
    public BillingCheckoutSession createCheckoutSession(UUID tenantId, String planCode) {
        String normalizedPlanCode = planCode.trim().toUpperCase(Locale.ROOT);
        String priceId = resolvePriceId(normalizedPlanCode);
        if (priceId == null || priceId.isBlank()) {
            throw new IllegalArgumentException(
                    "No Stripe price is configured for plan: " + normalizedPlanCode);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "subscription");
        form.add("client_reference_id", tenantId.toString());
        form.add("metadata[tenant_id]", tenantId.toString());
        form.add("metadata[plan_code]", normalizedPlanCode);
        form.add("subscription_data[metadata][tenant_id]", tenantId.toString());
        form.add("subscription_data[metadata][plan_code]", normalizedPlanCode);
        form.add("line_items[0][price]", priceId);
        form.add("line_items[0][quantity]", "1");
        form.add("success_url", properties.getSuccessUrl());
        form.add("cancel_url", properties.getCancelUrl());

        try {
            StripeCheckoutResponse response =
                    restClient
                            .post()
                            .uri("/v1/checkout/sessions")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(form)
                            .retrieve()
                            .body(StripeCheckoutResponse.class);
            if (response == null
                    || response.id() == null
                    || response.id().isBlank()
                    || response.url() == null
                    || response.url().isBlank()) {
                throw new BillingProviderException(
                        "Stripe returned an incomplete checkout session", null);
            }
            return new BillingCheckoutSession(
                    response.id(), response.url(), BillingProviderType.STRIPE);
        } catch (RestClientException ex) {
            throw new BillingProviderException("Stripe checkout session creation failed", ex);
        }
    }

    @Override
    public BillingProviderSubscriptionSnapshot fetchSubscription(String providerSubscriptionId) {
        String subscriptionId = requireSubscriptionId(providerSubscriptionId);

        try {
            JsonNode subscription =
                    restClient
                            .get()
                            .uri("/v1/subscriptions/{subscriptionId}", subscriptionId)
                            .retrieve()
                            .body(JsonNode.class);
            if (subscription == null || !subscription.isObject()) {
                throw new BillingProviderException(
                        "Stripe returned an incomplete subscription", null);
            }

            JsonNode periodSource = periodSource(subscription);
            JsonNode item = firstSubscriptionItem(subscription);
            String priceId = requiredText(requiredObject(item, "price"), "id");

            return new BillingProviderSubscriptionSnapshot(
                    BillingProviderType.STRIPE,
                    requiredText(subscription, "id"),
                    resolvePlanCode(priceId),
                    mapStatus(requiredText(subscription, "status")),
                    requiredInstant(periodSource, "current_period_start"),
                    requiredInstant(periodSource, "current_period_end"),
                    optionalBoolean(subscription, "cancel_at_period_end"));
        } catch (RestClientException ex) {
            throw new BillingProviderException("Stripe subscription lookup failed", ex);
        }
    }

    @Override
    public void cancelSubscription(String providerSubscriptionId) {
        String subscriptionId = requireSubscriptionId(providerSubscriptionId);
        try {
            restClient
                    .delete()
                    .uri("/v1/subscriptions/{subscriptionId}", subscriptionId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BillingProviderException("Stripe subscription cancellation failed", ex);
        }
    }

    private JsonNode periodSource(JsonNode subscription) {
        if (hasIntegralNumber(subscription, "current_period_start")
                && hasIntegralNumber(subscription, "current_period_end")) {
            return subscription;
        }
        return firstSubscriptionItem(subscription);
    }

    private JsonNode firstSubscriptionItem(JsonNode subscription) {
        JsonNode data = requiredObject(subscription, "items").get("data");
        if (data == null || !data.isArray() || data.size() == 0) {
            throw new BillingProviderException(
                    "Stripe returned a subscription without line items", null);
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
                    throw new BillingProviderException(
                            "Unsupported Stripe subscription status: " + status, null);
        };
    }

    private String resolvePriceId(String normalizedPlanCode) {
        for (Map.Entry<String, String> price : properties.getPrices().entrySet()) {
            if (price.getKey().equalsIgnoreCase(normalizedPlanCode)) {
                return price.getValue();
            }
        }
        return null;
    }

    private String resolvePlanCode(String priceId) {
        for (Map.Entry<String, String> price : properties.getPrices().entrySet()) {
            if (priceId.equals(price.getValue())) {
                return price.getKey().toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }

    private String requireSubscriptionId(String providerSubscriptionId) {
        if (providerSubscriptionId == null || providerSubscriptionId.isBlank()) {
            throw new IllegalArgumentException("providerSubscriptionId must not be blank");
        }
        return providerSubscriptionId.trim();
    }

    private JsonNode requiredObject(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isObject()) {
            throw new BillingProviderException(
                    "Stripe returned a subscription without " + field, null);
        }
        return value;
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new BillingProviderException(
                    "Stripe returned a subscription without " + field, null);
        }
        return value.asString();
    }

    private Instant requiredInstant(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber()) {
            throw new BillingProviderException(
                    "Stripe returned a subscription without " + field, null);
        }
        return Instant.ofEpochSecond(value.asLong());
    }

    private boolean hasIntegralNumber(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isIntegralNumber();
    }

    private boolean optionalBoolean(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isBoolean() && value.asBoolean();
    }

    private static void validateConfiguration(StripeBillingProperties properties) {
        if (properties == null) {
            throw new IllegalStateException("Stripe billing properties are required");
        }
        requireConfigured(properties.getSecretKey(), "STRIPE_SECRET_KEY");
        requireConfigured(properties.getBaseUrl(), "STRIPE_BASE_URL");
        requireConfigured(properties.getSuccessUrl(), "STRIPE_SUCCESS_URL");
        requireConfigured(properties.getCancelUrl(), "STRIPE_CANCEL_URL");
    }

    private static void requireConfigured(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    environmentVariable + " must be configured for Stripe billing");
        }
    }

    private record StripeCheckoutResponse(String id, String url) {}
}
