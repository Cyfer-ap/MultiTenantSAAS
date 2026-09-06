package com.chacha.multitenantsaas.billing.stripe;

import com.chacha.multitenantsaas.billing.catalog.SubscriptionPlanCatalogProvisioner;
import com.chacha.multitenantsaas.billing.entity.BillingProviderEnvironment;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanProviderMapping;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanProviderMappingStatus;
import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.repository.SubscriptionPlanProviderMappingRepository;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.entity.BillingInterval;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.repository.SubscriptionPlanRepository;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Currency;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
public class StripePlanCatalogProvisioner implements SubscriptionPlanCatalogProvisioner {

    private final StripeBillingProperties properties;
    private final SubscriptionPlanProviderMappingRepository mappingRepository;
    private final SubscriptionPlanRepository planRepository;
    private final RestClient restClient;

    @Autowired
    public StripePlanCatalogProvisioner(
            StripeBillingProperties properties,
            SubscriptionPlanProviderMappingRepository mappingRepository,
            SubscriptionPlanRepository planRepository) {
        this(properties, mappingRepository, planRepository, RestClient.builder());
    }

    StripePlanCatalogProvisioner(
            StripeBillingProperties properties,
            SubscriptionPlanProviderMappingRepository mappingRepository,
            SubscriptionPlanRepository planRepository,
            RestClient.Builder builder) {
        validateConfiguration(properties);
        this.properties = properties;
        this.mappingRepository = Objects.requireNonNull(mappingRepository);
        this.planRepository = Objects.requireNonNull(planRepository);
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
        if (!requiresProviderPrice(plan)) {
            return;
        }
        provisionNewProduct(plan);
    }

    @Override
    public void planUpdated(SubscriptionPlanResponse before, SubscriptionPlanResponse after) {
        Objects.requireNonNull(before, "before plan must not be null");
        Objects.requireNonNull(after, "after plan must not be null");

        SubscriptionPlanProviderMapping active = activeMapping(after);
        if (active == null) {
            active = importLegacyOrLatestMapping(after);
        }

        if (!requiresProviderPrice(after)) {
            if (active != null) {
                updateProductIfNeeded(before, after, active.getProviderProductId());
                deactivateAndArchive(active);
            }
            return;
        }

        if (active == null) {
            provisionNewProduct(after);
            return;
        }

        updateProductIfNeeded(before, after, active.getProviderProductId());
        if (commercialTermsChanged(before, after)) {
            replacePrice(after, active);
        }
    }

    private void provisionNewProduct(SubscriptionPlanResponse plan) {
        String productId = createProduct(plan);
        try {
            String priceId = createPrice(plan, productId);
            SubscriptionPlanProviderMapping mapping = newMapping(plan, productId, priceId);
            try {
                mappingRepository.saveAndFlush(mapping);
            } catch (RuntimeException ex) {
                compensateNewCatalogObjects(productId, priceId, ex);
                throw ex;
            }
        } catch (RuntimeException ex) {
            if (!(ex instanceof BillingProviderException)) {
                setProductActiveQuietly(productId, false, ex);
            } else {
                setProductActiveQuietly(productId, false, ex);
            }
            throw ex;
        }
    }

    private SubscriptionPlanProviderMapping importLegacyOrLatestMapping(
            SubscriptionPlanResponse plan) {
        List<SubscriptionPlanProviderMapping> history =
                mappingRepository.findAllByPlan_IdAndProviderAndEnvironmentOrderByCreatedAtDesc(
                        plan.id(), providerType(), environment());
        if (!history.isEmpty()) {
            SubscriptionPlanProviderMapping latest = history.getFirst();
            if (latest.getProviderProductId() != null
                    && !latest.getProviderProductId().isBlank()
                    && latest.getProviderPriceId() != null
                    && !latest.getProviderPriceId().isBlank()) {
                if (latest.getStatus() == SubscriptionPlanProviderMappingStatus.ARCHIVED) {
                    return null;
                }
                return latest;
            }
        }

        String legacyPriceId = configuredPriceId(plan.code());
        if (legacyPriceId == null || legacyPriceId.isBlank()) {
            return null;
        }

        return mappingRepository
                .findFirstByProviderAndEnvironmentAndProviderPriceId(
                        providerType(), environment(), legacyPriceId)
                .orElseGet(() -> importLegacyMapping(plan, legacyPriceId));
    }

    private SubscriptionPlanProviderMapping importLegacyMapping(
            SubscriptionPlanResponse plan, String legacyPriceId) {
        JsonNode price = fetchPrice(legacyPriceId);
        String returnedPriceId = requiredText(price, "id", "Stripe price");
        if (!legacyPriceId.equals(returnedPriceId)) {
            throw new BillingProviderException("Stripe returned the wrong legacy price", null);
        }
        String productId = requiredText(price, "product", "Stripe price");
        SubscriptionPlanProviderMapping mapping = newMapping(plan, productId, legacyPriceId);
        return mappingRepository.saveAndFlush(mapping);
    }

    private void replacePrice(
            SubscriptionPlanResponse plan, SubscriptionPlanProviderMapping currentMapping) {
        String productId = requireProviderProductId(currentMapping);
        String currentPriceId = requireProviderPriceId(currentMapping);
        String newPriceId = createPrice(plan, productId);

        try {
            setPriceActive(currentPriceId, false);
        } catch (RuntimeException ex) {
            setPriceActiveQuietly(newPriceId, false, ex);
            throw ex;
        }

        try {
            currentMapping.archive(Instant.now());
            mappingRepository.save(currentMapping);
            mappingRepository.saveAndFlush(newMapping(plan, productId, newPriceId));
        } catch (RuntimeException ex) {
            setPriceActiveQuietly(currentPriceId, true, ex);
            setPriceActiveQuietly(newPriceId, false, ex);
            throw ex;
        }
    }

    private void deactivateAndArchive(SubscriptionPlanProviderMapping mapping) {
        String priceId = requireProviderPriceId(mapping);
        setPriceActive(priceId, false);
        try {
            mapping.archive(Instant.now());
            mappingRepository.saveAndFlush(mapping);
        } catch (RuntimeException ex) {
            setPriceActiveQuietly(priceId, true, ex);
            throw ex;
        }
    }

    private void updateProductIfNeeded(
            SubscriptionPlanResponse before,
            SubscriptionPlanResponse after,
            String providerProductId) {
        if (Objects.equals(before.name(), after.name())
                && Objects.equals(before.description(), after.description())) {
            return;
        }
        updateProduct(after, providerProductId);
    }

    private String createProduct(SubscriptionPlanResponse plan) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("name", plan.name());
        if (plan.description() != null && !plan.description().isBlank()) {
            form.add("description", plan.description());
        }
        form.add("metadata[local_plan_id]", plan.id().toString());
        form.add("metadata[plan_code]", plan.code());

        JsonNode response =
                postForm(
                        "/v1/products",
                        form,
                        idempotencyKey("product", plan.id().toString()),
                        "Stripe product creation failed");
        return requiredText(response, "id", "Stripe product");
    }

    private void updateProduct(SubscriptionPlanResponse plan, String providerProductId) {
        String productId = requireText(providerProductId, "Stripe product id");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("name", plan.name());
        form.add("description", plan.description() == null ? "" : plan.description());
        form.add("metadata[local_plan_id]", plan.id().toString());
        form.add("metadata[plan_code]", plan.code());

        postForm(
                "/v1/products/" + productId,
                form,
                idempotencyKey(
                        "product-update",
                        plan.id() + "|" + plan.name() + "|" + Objects.toString(plan.description(), "")),
                "Stripe product update failed");
    }

    private String createPrice(SubscriptionPlanResponse plan, String providerProductId) {
        String productId = requireText(providerProductId, "Stripe product id");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("product", productId);
        form.add("currency", plan.currency().toLowerCase(Locale.ROOT));
        form.add("unit_amount", toMinorUnits(plan));
        form.add("recurring[interval]", stripeInterval(plan.billingInterval()));
        form.add("metadata[local_plan_id]", plan.id().toString());
        form.add("metadata[plan_code]", plan.code());

        JsonNode response =
                postForm(
                        "/v1/prices",
                        form,
                        idempotencyKey("price", commercialFingerprint(plan)),
                        "Stripe price creation failed");
        return requiredText(response, "id", "Stripe price");
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

    private void setPriceActive(String providerPriceId, boolean active) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("active", Boolean.toString(active));
        postForm(
                "/v1/prices/" + requireText(providerPriceId, "Stripe price id"),
                form,
                idempotencyKey("price-active", providerPriceId + "|" + active),
                "Stripe price update failed");
    }

    private void setProductActive(String providerProductId, boolean active) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("active", Boolean.toString(active));
        postForm(
                "/v1/products/" + requireText(providerProductId, "Stripe product id"),
                form,
                idempotencyKey("product-active", providerProductId + "|" + active),
                "Stripe product update failed");
    }

    private JsonNode postForm(
            String uri, MultiValueMap<String, String> form, String idempotencyKey, String failure) {
        try {
            JsonNode response =
                    restClient
                            .post()
                            .uri(uri)
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(form)
                            .retrieve()
                            .body(JsonNode.class);
            if (response == null || !response.isObject()) {
                throw new BillingProviderException(failure + ": incomplete response", null);
            }
            return response;
        } catch (RestClientException ex) {
            throw new BillingProviderException(failure, ex);
        }
    }

    private SubscriptionPlanProviderMapping activeMapping(SubscriptionPlanResponse plan) {
        return mappingRepository
                .findFirstByPlan_IdAndProviderAndEnvironmentAndStatusOrderByCreatedAtDesc(
                        plan.id(),
                        providerType(),
                        environment(),
                        SubscriptionPlanProviderMappingStatus.ACTIVE)
                .orElse(null);
    }

    private SubscriptionPlanProviderMapping newMapping(
            SubscriptionPlanResponse plan, String productId, String priceId) {
        SubscriptionPlan planEntity =
                planRepository
                        .findById(plan.id())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Subscription plan disappeared during Stripe provisioning"));
        return new SubscriptionPlanProviderMapping(
                planEntity, providerType(), environment(), productId, priceId, null);
    }

    private String configuredPriceId(String planCode) {
        for (var entry : properties.getPrices().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(planCode)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean requiresProviderPrice(SubscriptionPlanResponse plan) {
        return plan.price() != null && plan.price().signum() > 0;
    }

    private boolean commercialTermsChanged(
            SubscriptionPlanResponse before, SubscriptionPlanResponse after) {
        return before.price().compareTo(after.price()) != 0
                || !before.currency().equalsIgnoreCase(after.currency())
                || before.billingInterval() != after.billingInterval();
    }

    private String toMinorUnits(SubscriptionPlanResponse plan) {
        try {
            Currency currency = Currency.getInstance(plan.currency().toUpperCase(Locale.ROOT));
            int digits = currency.getDefaultFractionDigits();
            if (digits < 0) {
                throw new IllegalArgumentException("Unsupported currency: " + plan.currency());
            }
            return plan.price()
                    .movePointRight(digits)
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .toBigIntegerExact()
                    .toString();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Plan price cannot be represented in Stripe minor units for currency "
                            + plan.currency(),
                    ex);
        }
    }

    private String stripeInterval(BillingInterval interval) {
        return switch (interval) {
            case MONTHLY -> "month";
            case YEARLY -> "year";
        };
    }

    private String commercialFingerprint(SubscriptionPlanResponse plan) {
        return plan.id()
                + "|"
                + plan.price().toPlainString()
                + "|"
                + plan.currency().toUpperCase(Locale.ROOT)
                + "|"
                + plan.billingInterval();
    }

    private String idempotencyKey(String operation, String material) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            String suffix = HexFormat.of().formatHex(hash, 0, 12);
            return "mtsaas-" + environment().name().toLowerCase(Locale.ROOT) + "-" + operation + "-" + suffix;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private BillingProviderEnvironment environment() {
        return properties.getEnvironment();
    }

    private String requireProviderProductId(SubscriptionPlanProviderMapping mapping) {
        return requireText(mapping.getProviderProductId(), "Stripe product id");
    }

    private String requireProviderPriceId(SubscriptionPlanProviderMapping mapping) {
        return requireText(mapping.getProviderPriceId(), "Stripe price id");
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

    private void compensateNewCatalogObjects(String productId, String priceId, RuntimeException cause) {
        setPriceActiveQuietly(priceId, false, cause);
        setProductActiveQuietly(productId, false, cause);
    }

    private void setPriceActiveQuietly(String priceId, boolean active, RuntimeException cause) {
        try {
            setPriceActive(priceId, active);
        } catch (RuntimeException compensationFailure) {
            cause.addSuppressed(compensationFailure);
        }
    }

    private void setProductActiveQuietly(String productId, boolean active, RuntimeException cause) {
        try {
            setProductActive(productId, active);
        } catch (RuntimeException compensationFailure) {
            cause.addSuppressed(compensationFailure);
        }
    }

    private static void validateConfiguration(StripeBillingProperties properties) {
        if (properties == null) {
            throw new IllegalStateException("Stripe billing properties are required");
        }
        if (properties.getEnvironment() == null) {
            throw new IllegalStateException("STRIPE_BILLING_ENVIRONMENT must be configured");
        }
        requireConfigured(properties.getSecretKey(), "STRIPE_SECRET_KEY");
        requireConfigured(properties.getBaseUrl(), "STRIPE_BASE_URL");
    }

    private static void requireConfigured(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentVariable + " must be configured for Stripe billing");
        }
    }
}
