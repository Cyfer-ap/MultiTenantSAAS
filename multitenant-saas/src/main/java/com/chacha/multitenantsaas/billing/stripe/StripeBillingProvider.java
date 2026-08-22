package com.chacha.multitenantsaas.billing.stripe;

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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "app.billing.stripe", name = "enabled", havingValue = "true")
public class StripeBillingProvider implements BillingProvider {

    private final StripeBillingProperties properties;
    private final RestClient restClient;

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
    public void cancelSubscription(String providerSubscriptionId) {
        if (providerSubscriptionId == null || providerSubscriptionId.isBlank()) {
            throw new IllegalArgumentException("providerSubscriptionId must not be blank");
        }
        try {
            restClient
                    .delete()
                    .uri("/v1/subscriptions/{subscriptionId}", providerSubscriptionId.trim())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BillingProviderException("Stripe subscription cancellation failed", ex);
        }
    }

    private String resolvePriceId(String normalizedPlanCode) {
        for (Map.Entry<String, String> price : properties.getPrices().entrySet()) {
            if (price.getKey().equalsIgnoreCase(normalizedPlanCode)) {
                return price.getValue();
            }
        }
        return null;
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
