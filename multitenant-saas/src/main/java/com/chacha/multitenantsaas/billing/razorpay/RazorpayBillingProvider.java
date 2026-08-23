package com.chacha.multitenantsaas.billing.razorpay;

import com.chacha.multitenantsaas.billing.provider.BillingCheckoutSession;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.billing.provider.BillingProviderSubscriptionSnapshot;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
@ConditionalOnProperty(prefix = "app.billing.razorpay", name = "enabled", havingValue = "true")
public class RazorpayBillingProvider implements BillingProvider {

    private final RazorpayBillingProperties properties;
    private final RestClient restClient;

    @Autowired
    public RazorpayBillingProvider(RazorpayBillingProperties properties) {
        this(properties, RestClient.builder());
    }

    RazorpayBillingProvider(RazorpayBillingProperties properties, RestClient.Builder builder) {
        validateConfiguration(properties);
        this.properties = properties;
        this.restClient =
                builder.baseUrl(properties.getBaseUrl())
                        .defaultHeaders(
                                headers ->
                                        headers.setBasicAuth(
                                                properties.getKeyId(), properties.getKeySecret()))
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .build();
    }

    @Override
    public BillingProviderType providerType() {
        return BillingProviderType.RAZORPAY;
    }

    @Override
    public BillingCheckoutSession createCheckoutSession(UUID tenantId, String planCode) {
        String normalizedPlanCode = planCode.trim().toUpperCase(Locale.ROOT);
        String providerPlanId = resolvePlanId(normalizedPlanCode);
        if (providerPlanId == null || providerPlanId.isBlank()) {
            throw new IllegalArgumentException(
                    "No Razorpay plan is configured for plan: " + normalizedPlanCode);
        }

        RazorpaySubscriptionRequest request =
                new RazorpaySubscriptionRequest(
                        providerPlanId,
                        properties.getTotalCount(),
                        1,
                        false,
                        Map.of("tenant_id", tenantId.toString(), "plan_code", normalizedPlanCode));

        try {
            RazorpaySubscriptionResponse response =
                    restClient
                            .post()
                            .uri("/v1/subscriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(RazorpaySubscriptionResponse.class);
            if (response == null
                    || response.id() == null
                    || response.id().isBlank()
                    || response.shortUrl() == null
                    || response.shortUrl().isBlank()) {
                throw new BillingProviderException(
                        "Razorpay returned an incomplete subscription link", null);
            }
            return new BillingCheckoutSession(
                    response.id(), response.shortUrl(), BillingProviderType.RAZORPAY);
        } catch (RestClientException ex) {
            throw new BillingProviderException("Razorpay subscription creation failed", ex);
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
                        "Razorpay returned an incomplete subscription", null);
            }

            return new BillingProviderSubscriptionSnapshot(
                    BillingProviderType.RAZORPAY,
                    requiredText(subscription, "id"),
                    resolvePlanCode(requiredText(subscription, "plan_id")),
                    mapStatus(requiredText(subscription, "status")),
                    requiredInstant(subscription, "current_start"),
                    requiredInstant(subscription, "current_end"),
                    optionalBoolean(subscription, "cancel_at_cycle_end"));
        } catch (RestClientException ex) {
            throw new BillingProviderException("Razorpay subscription lookup failed", ex);
        }
    }

    @Override
    public void cancelSubscription(String providerSubscriptionId) {
        String subscriptionId = requireSubscriptionId(providerSubscriptionId);

        try {
            restClient
                    .post()
                    .uri("/v1/subscriptions/{subscriptionId}/cancel", subscriptionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RazorpayCancellationRequest(false))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BillingProviderException("Razorpay subscription cancellation failed", ex);
        }
    }

    private TenantSubscriptionStatus mapStatus(String status) {
        return switch (status) {
            case "created", "authenticated" -> TenantSubscriptionStatus.TRIALING;
            case "active" -> TenantSubscriptionStatus.ACTIVE;
            case "pending", "halted" -> TenantSubscriptionStatus.PAST_DUE;
            case "cancelled" -> TenantSubscriptionStatus.CANCELLED;
            case "completed", "expired" -> TenantSubscriptionStatus.EXPIRED;
            default ->
                    throw new BillingProviderException(
                            "Unsupported Razorpay subscription status: " + status, null);
        };
    }

    private String resolvePlanId(String normalizedPlanCode) {
        for (Map.Entry<String, String> plan : properties.getPlans().entrySet()) {
            if (plan.getKey().equalsIgnoreCase(normalizedPlanCode)) {
                return plan.getValue();
            }
        }
        return null;
    }

    private String resolvePlanCode(String providerPlanId) {
        for (Map.Entry<String, String> plan : properties.getPlans().entrySet()) {
            if (providerPlanId.equals(plan.getValue())) {
                return plan.getKey().toUpperCase(Locale.ROOT);
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

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new BillingProviderException(
                    "Razorpay returned a subscription without " + field, null);
        }
        return value.asString();
    }

    private Instant requiredInstant(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber()) {
            throw new BillingProviderException(
                    "Razorpay returned a subscription without " + field, null);
        }
        return Instant.ofEpochSecond(value.asLong());
    }

    private boolean optionalBoolean(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isBoolean() && value.asBoolean();
    }

    private static void validateConfiguration(RazorpayBillingProperties properties) {
        if (properties == null) {
            throw new IllegalStateException("Razorpay billing properties are required");
        }
        requireConfigured(properties.getKeyId(), "RAZORPAY_KEY_ID");
        requireConfigured(properties.getKeySecret(), "RAZORPAY_KEY_SECRET");
        requireConfigured(properties.getBaseUrl(), "RAZORPAY_BASE_URL");
        if (properties.getTotalCount() < 1) {
            throw new IllegalStateException("RAZORPAY_SUBSCRIPTION_TOTAL_COUNT must be at least 1");
        }
    }

    private static void requireConfigured(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    environmentVariable + " must be configured for Razorpay billing");
        }
    }

    private record RazorpaySubscriptionRequest(
            @JsonProperty("plan_id") String planId,
            @JsonProperty("total_count") int totalCount,
            int quantity,
            @JsonProperty("customer_notify") boolean customerNotify,
            Map<String, String> notes) {}

    private record RazorpaySubscriptionResponse(
            String id, @JsonProperty("short_url") String shortUrl) {}

    private record RazorpayCancellationRequest(
            @JsonProperty("cancel_at_cycle_end") boolean cancelAtCycleEnd) {}
}
