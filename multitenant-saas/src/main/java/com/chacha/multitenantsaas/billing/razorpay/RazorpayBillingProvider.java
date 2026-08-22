package com.chacha.multitenantsaas.billing.razorpay;

import com.chacha.multitenantsaas.billing.provider.BillingCheckoutSession;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "app.billing.razorpay", name = "enabled", havingValue = "true")
public class RazorpayBillingProvider implements BillingProvider {

    private final RazorpayBillingProperties properties;
    private final RestClient restClient;

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
                                                properties.getKeyId(),
                                                properties.getKeySecret()))
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
                        Map.of(
                                "tenant_id", tenantId.toString(),
                                "plan_code", normalizedPlanCode));

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
            throw new BillingProviderException(
                    "Razorpay subscription creation failed", ex);
        }
    }

    @Override
    public void cancelSubscription(String providerSubscriptionId) {
        if (providerSubscriptionId == null || providerSubscriptionId.isBlank()) {
            throw new IllegalArgumentException("providerSubscriptionId must not be blank");
        }

        try {
            restClient
                    .post()
                    .uri(
                            "/v1/subscriptions/{subscriptionId}/cancel",
                            providerSubscriptionId.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RazorpayCancellationRequest(false))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BillingProviderException(
                    "Razorpay subscription cancellation failed", ex);
        }
    }

    private String resolvePlanId(String normalizedPlanCode) {
        for (Map.Entry<String, String> plan : properties.getPlans().entrySet()) {
            if (plan.getKey().equalsIgnoreCase(normalizedPlanCode)) {
                return plan.getValue();
            }
        }
        return null;
    }

    private static void validateConfiguration(RazorpayBillingProperties properties) {
        if (properties == null) {
            throw new IllegalStateException("Razorpay billing properties are required");
        }
        requireConfigured(properties.getKeyId(), "RAZORPAY_KEY_ID");
        requireConfigured(properties.getKeySecret(), "RAZORPAY_KEY_SECRET");
        requireConfigured(properties.getBaseUrl(), "RAZORPAY_BASE_URL");
        if (properties.getTotalCount() < 1) {
            throw new IllegalStateException(
                    "RAZORPAY_SUBSCRIPTION_TOTAL_COUNT must be at least 1");
        }
    }

    private static void requireConfigured(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    environmentVariable + " must be configured for Razorpay billing");
        }
    }

    private record RazorpaySubscriptionRequest(
            String planId,
            int totalCount,
            int quantity,
            boolean customerNotify,
            Map<String, String> notes) {}

    private record RazorpaySubscriptionResponse(String id, String shortUrl) {}

    private record RazorpayCancellationRequest(boolean cancelAtCycleEnd) {}
}
