package com.chacha.multitenantsaas.billing.stripe;

import com.chacha.multitenantsaas.billing.catalog.SubscriptionPlanCatalogProvisioner;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanProviderMapping;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanProviderMappingStatus;
import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.repository.SubscriptionPlanProviderMappingRepository;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.repository.SubscriptionPlanRepository;
import java.time.Instant;
import java.util.Map;
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
public class StripePlanRetirementProvisioner implements SubscriptionPlanCatalogProvisioner {

    private final StripeBillingProperties properties;
    private final SubscriptionPlanProviderMappingRepository mappingRepository;
    private final SubscriptionPlanRepository planRepository;
    private final RestClient restClient;

    @Autowired
    public StripePlanRetirementProvisioner(
            StripeBillingProperties properties,
            SubscriptionPlanProviderMappingRepository mappingRepository,
            SubscriptionPlanRepository planRepository) {
        this(properties, mappingRepository, planRepository, RestClient.builder());
    }

    StripePlanRetirementProvisioner(
            StripeBillingProperties properties,
            SubscriptionPlanProviderMappingRepository mappingRepository,
            SubscriptionPlanRepository planRepository,
            RestClient.Builder builder) {
        this.properties = properties;
        this.mappingRepository = mappingRepository;
        this.planRepository = planRepository;
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
    public void planCreated(SubscriptionPlanResponse plan) {
        // Product/Price provisioning is handled by StripePlanCatalogProvisioner.
    }

    @Override
    public void planUpdated(SubscriptionPlanResponse before, SubscriptionPlanResponse after) {
        // Product/Price synchronization is handled by StripePlanCatalogProvisioner.
    }

    @Override
    public void planRetired(SubscriptionPlanResponse plan) {
        SubscriptionPlanProviderMapping mapping = activeMapping(plan);
        if (mapping == null) {
            mapping = importLegacyMapping(plan);
        }
        if (mapping == null) {
            return;
        }

        String priceId = requireText(mapping.getProviderPriceId(), "Stripe price id");
        String productId = requireText(mapping.getProviderProductId(), "Stripe product id");

        setActive("/v1/prices/" + priceId, false, "Stripe price retirement failed");
        setActive("/v1/products/" + productId, false, "Stripe product retirement failed");

        if (mapping.getStatus() != SubscriptionPlanProviderMappingStatus.ARCHIVED) {
            mapping.archive(Instant.now());
            mappingRepository.saveAndFlush(mapping);
        }
    }

    private SubscriptionPlanProviderMapping activeMapping(SubscriptionPlanResponse plan) {
        return mappingRepository
                .findFirstByPlan_IdAndProviderAndEnvironmentAndStatusOrderByCreatedAtDesc(
                        plan.id(),
                        BillingProviderType.STRIPE,
                        properties.getEnvironment(),
                        SubscriptionPlanProviderMappingStatus.ACTIVE)
                .orElse(null);
    }

    private SubscriptionPlanProviderMapping importLegacyMapping(SubscriptionPlanResponse plan) {
        String priceId = configuredPriceId(plan.code());
        if (priceId == null || priceId.isBlank()) {
            return null;
        }

        SubscriptionPlanProviderMapping existing =
                mappingRepository
                        .findFirstByProviderAndEnvironmentAndProviderPriceId(
                                BillingProviderType.STRIPE, properties.getEnvironment(), priceId)
                        .orElse(null);
        if (existing != null) {
            return existing;
        }

        JsonNode price = fetchPrice(priceId);
        String returnedPriceId = requiredText(price, "id", "Stripe price");
        if (!priceId.equals(returnedPriceId)) {
            throw new BillingProviderException("Stripe returned the wrong legacy price", null);
        }
        String productId = requiredText(price, "product", "Stripe price");
        SubscriptionPlan planEntity =
                planRepository
                        .findById(plan.id())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Subscription plan disappeared during retirement"));
        return mappingRepository.saveAndFlush(
                new SubscriptionPlanProviderMapping(
                        planEntity,
                        BillingProviderType.STRIPE,
                        properties.getEnvironment(),
                        productId,
                        priceId,
                        null));
    }

    private JsonNode fetchPrice(String providerPriceId) {
        try {
            JsonNode response =
                    restClient
                            .get()
                            .uri("/v1/prices/{priceId}", providerPriceId)
                            .retrieve()
                            .body(JsonNode.class);
            if (response == null || !response.isObject()) {
                throw new BillingProviderException("Stripe returned an incomplete price", null);
            }
            return response;
        } catch (RestClientException ex) {
            throw new BillingProviderException("Stripe price lookup failed", ex);
        }
    }

    private void setActive(String uri, boolean active, String failureMessage) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("active", Boolean.toString(active));
        try {
            restClient
                    .post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BillingProviderException(failureMessage, ex);
        }
    }

    private String configuredPriceId(String planCode) {
        for (Map.Entry<String, String> entry : properties.getPrices().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(planCode)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BillingProviderException(field + " is missing", null);
        }
        return value.trim();
    }

    private String requiredText(JsonNode node, String field, String resource) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new BillingProviderException(resource + " response is missing " + field, null);
        }
        return value.asString();
    }
}
