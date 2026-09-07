package com.chacha.multitenantsaas.billing.razorpay;

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
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Currency;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
public class RazorpayPlanCatalogProvisioner implements SubscriptionPlanCatalogProvisioner {

    private static final int PLAN_SCAN_PAGE_SIZE = 100;
    private static final int MAX_PLAN_SCAN_PAGES = 20;
    private static final BigInteger MAX_RAZORPAY_AMOUNT = new BigInteger("4294967295");

    private final RazorpayBillingProperties properties;
    private final SubscriptionPlanProviderMappingRepository mappingRepository;
    private final SubscriptionPlanRepository planRepository;
    private final RestClient restClient;

    @Autowired
    public RazorpayPlanCatalogProvisioner(
            RazorpayBillingProperties properties,
            SubscriptionPlanProviderMappingRepository mappingRepository,
            SubscriptionPlanRepository planRepository) {
        this(properties, mappingRepository, planRepository, RestClient.builder());
    }

    RazorpayPlanCatalogProvisioner(
            RazorpayBillingProperties properties,
            SubscriptionPlanProviderMappingRepository mappingRepository,
            SubscriptionPlanRepository planRepository,
            RestClient.Builder builder) {
        validateConfiguration(properties);
        this.properties = properties;
        this.mappingRepository = Objects.requireNonNull(mappingRepository);
        this.planRepository = Objects.requireNonNull(planRepository);
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
    public void planCreated(SubscriptionPlanResponse plan) {
        if (requiresProviderPlan(plan)) {
            provisionNewPlan(plan);
        }
    }

    @Override
    public void planUpdated(SubscriptionPlanResponse before, SubscriptionPlanResponse after) {
        Objects.requireNonNull(before, "before plan must not be null");
        Objects.requireNonNull(after, "after plan must not be null");

        SubscriptionPlanProviderMapping active = activeMapping(after);
        if (active == null) {
            active = importLegacyOrLatestMapping(after);
        }

        if (!requiresProviderPlan(after)) {
            if (active != null) {
                archive(active);
            }
            return;
        }

        if (active == null) {
            provisionNewPlan(after);
            return;
        }

        if (providerRepresentationChanged(before, after)) {
            replacePlan(after, active);
        }
    }

    @Override
    public void planRetired(SubscriptionPlanResponse plan) {
        SubscriptionPlanProviderMapping active = activeMapping(plan);
        if (active == null) {
            active = importLegacyOrLatestMapping(plan);
        }
        if (active != null) {
            archive(active);
        }
    }

    private void provisionNewPlan(SubscriptionPlanResponse plan) {
        String providerPlanId = findManagedPlanId(plan);
        if (providerPlanId == null) {
            providerPlanId = createPlan(plan);
        }
        persistNewMapping(plan, providerPlanId);
    }

    private void replacePlan(
            SubscriptionPlanResponse plan, SubscriptionPlanProviderMapping currentMapping) {
        String providerPlanId = findManagedPlanId(plan);
        if (providerPlanId == null) {
            providerPlanId = createPlan(plan);
        }

        String currentPlanId = requireProviderPlanId(currentMapping);
        if (currentPlanId.equals(providerPlanId)) {
            return;
        }

        SubscriptionPlanProviderMapping replacement = newMapping(plan, providerPlanId);
        mappingRepository.saveAndFlush(replacement);
        archive(currentMapping);
    }

    private SubscriptionPlanProviderMapping importLegacyOrLatestMapping(
            SubscriptionPlanResponse plan) {
        List<SubscriptionPlanProviderMapping> history =
                mappingRepository.findAllByPlan_IdAndProviderAndEnvironmentOrderByCreatedAtDesc(
                        plan.id(), providerType(), environment());
        if (!history.isEmpty()) {
            SubscriptionPlanProviderMapping latest = history.getFirst();
            if (latest.getStatus() == SubscriptionPlanProviderMappingStatus.ARCHIVED) {
                return null;
            }
            if (latest.getProviderPlanId() != null && !latest.getProviderPlanId().isBlank()) {
                return latest;
            }
        }

        String legacyPlanId = configuredPlanId(plan.code());
        if (legacyPlanId == null || legacyPlanId.isBlank()) {
            return null;
        }

        return mappingRepository
                .findFirstByProviderAndEnvironmentAndProviderPlanId(
                        providerType(), environment(), legacyPlanId)
                .orElseGet(() -> importLegacyMapping(plan, legacyPlanId));
    }

    private SubscriptionPlanProviderMapping importLegacyMapping(
            SubscriptionPlanResponse plan, String legacyPlanId) {
        JsonNode providerPlan = fetchPlan(legacyPlanId);
        String returnedPlanId = requiredText(providerPlan, "id", "Razorpay plan");
        if (!legacyPlanId.equals(returnedPlanId)) {
            throw new BillingProviderException("Razorpay returned the wrong legacy plan", null);
        }

        SubscriptionPlanProviderMapping mapping = newMapping(plan, legacyPlanId);
        return mappingRepository.saveAndFlush(mapping);
    }

    private String findManagedPlanId(SubscriptionPlanResponse plan) {
        String fingerprint = managedFingerprint(plan);
        for (int page = 0; page < MAX_PLAN_SCAN_PAGES; page++) {
            int skip = page * PLAN_SCAN_PAGE_SIZE;
            JsonNode response = fetchPlans(skip);
            JsonNode items = response.get("items");
            if (items == null || !items.isArray()) {
                throw new BillingProviderException(
                        "Razorpay returned an incomplete plan collection", null);
            }

            for (JsonNode candidate : items) {
                JsonNode notes = candidate.get("notes");
                if (notes == null || !notes.isObject()) {
                    continue;
                }
                JsonNode candidateFingerprint = notes.get("mtsaas_fingerprint");
                if (candidateFingerprint != null
                        && candidateFingerprint.isString()
                        && fingerprint.equals(candidateFingerprint.asString())) {
                    return requiredText(candidate, "id", "Razorpay plan");
                }
            }

            if (items.size() < PLAN_SCAN_PAGE_SIZE) {
                return null;
            }
        }

        throw new BillingProviderException(
                "Razorpay managed-plan lookup exceeded the safe scan limit", null);
    }

    private JsonNode fetchPlans(int skip) {
        try {
            JsonNode response =
                    restClient
                            .get()
                            .uri(
                                    uriBuilder ->
                                            uriBuilder
                                                    .path("/v1/plans")
                                                    .queryParam("count", PLAN_SCAN_PAGE_SIZE)
                                                    .queryParam("skip", skip)
                                                    .build())
                            .retrieve()
                            .body(JsonNode.class);
            if (response == null || !response.isObject()) {
                throw new BillingProviderException(
                        "Razorpay returned an incomplete plan collection", null);
            }
            return response;
        } catch (RestClientException ex) {
            throw new BillingProviderException("Razorpay plan lookup failed", ex);
        }
    }

    private JsonNode fetchPlan(String providerPlanId) {
        try {
            JsonNode response =
                    restClient
                            .get()
                            .uri("/v1/plans/{planId}", providerPlanId)
                            .retrieve()
                            .body(JsonNode.class);
            if (response == null || !response.isObject()) {
                throw new BillingProviderException("Razorpay returned an incomplete plan", null);
            }
            return response;
        } catch (RestClientException ex) {
            throw new BillingProviderException("Razorpay plan lookup failed", ex);
        }
    }

    private String createPlan(SubscriptionPlanResponse plan) {
        RazorpayPlanRequest request =
                new RazorpayPlanRequest(
                        razorpayPeriod(plan.billingInterval()),
                        1,
                        new RazorpayPlanItem(
                                plan.name(),
                                toMinorUnits(plan),
                                plan.currency().toUpperCase(Locale.ROOT),
                                plan.description()),
                        Map.of(
                                "mtsaas_local_plan_id",
                                plan.id().toString(),
                                "mtsaas_plan_code",
                                plan.code(),
                                "mtsaas_fingerprint",
                                managedFingerprint(plan)));
        try {
            JsonNode response =
                    restClient
                            .post()
                            .uri("/v1/plans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(JsonNode.class);
            if (response == null || !response.isObject()) {
                throw new BillingProviderException(
                        "Razorpay plan creation failed: incomplete response", null);
            }
            return requiredText(response, "id", "Razorpay plan");
        } catch (RestClientException ex) {
            throw new BillingProviderException("Razorpay plan creation failed", ex);
        }
    }

    private long toMinorUnits(SubscriptionPlanResponse plan) {
        try {
            Currency currency = Currency.getInstance(plan.currency().toUpperCase(Locale.ROOT));
            int digits = currency.getDefaultFractionDigits();
            if (digits < 0) {
                throw new IllegalArgumentException("Unsupported currency: " + plan.currency());
            }
            BigInteger amount =
                    plan.price()
                            .movePointRight(digits)
                            .setScale(0, RoundingMode.UNNECESSARY)
                            .toBigIntegerExact();
            if (amount.signum() <= 0 || amount.compareTo(MAX_RAZORPAY_AMOUNT) > 0) {
                throw new IllegalArgumentException(
                        "Razorpay plan amount is outside the supported provider range");
            }
            return amount.longValueExact();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Plan price cannot be represented in Razorpay currency subunits for currency "
                            + plan.currency(),
                    ex);
        }
    }

    private String razorpayPeriod(BillingInterval interval) {
        return switch (interval) {
            case MONTHLY -> "monthly";
            case YEARLY -> "yearly";
        };
    }

    private boolean requiresProviderPlan(SubscriptionPlanResponse plan) {
        return plan.price() != null && plan.price().signum() > 0;
    }

    private boolean providerRepresentationChanged(
            SubscriptionPlanResponse before, SubscriptionPlanResponse after) {
        return before.price().compareTo(after.price()) != 0
                || !before.currency().equalsIgnoreCase(after.currency())
                || before.billingInterval() != after.billingInterval()
                || !Objects.equals(before.name(), after.name())
                || !Objects.equals(before.description(), after.description());
    }

    private String managedFingerprint(SubscriptionPlanResponse plan) {
        String material =
                plan.id()
                        + "|"
                        + plan.name()
                        + "|"
                        + Objects.toString(plan.description(), "")
                        + "|"
                        + plan.price().toPlainString()
                        + "|"
                        + plan.currency().toUpperCase(Locale.ROOT)
                        + "|"
                        + plan.billingInterval();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
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

    private void persistNewMapping(SubscriptionPlanResponse plan, String providerPlanId) {
        mappingRepository.saveAndFlush(newMapping(plan, providerPlanId));
    }

    private SubscriptionPlanProviderMapping newMapping(
            SubscriptionPlanResponse plan, String providerPlanId) {
        SubscriptionPlan planEntity =
                planRepository
                        .findById(plan.id())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Subscription plan disappeared during Razorpay provisioning"));
        return new SubscriptionPlanProviderMapping(
                planEntity, providerType(), environment(), null, null, providerPlanId);
    }

    private void archive(SubscriptionPlanProviderMapping mapping) {
        mapping.archive(Instant.now());
        mappingRepository.saveAndFlush(mapping);
    }

    private String configuredPlanId(String planCode) {
        for (Map.Entry<String, String> entry : properties.getPlans().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(planCode)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String requireProviderPlanId(SubscriptionPlanProviderMapping mapping) {
        String providerPlanId = mapping.getProviderPlanId();
        if (providerPlanId == null || providerPlanId.isBlank()) {
            throw new BillingProviderException("Razorpay plan id is missing", null);
        }
        return providerPlanId.trim();
    }

    private String requiredText(JsonNode node, String field, String resource) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new BillingProviderException(resource + " response is missing " + field, null);
        }
        return value.asString();
    }

    private BillingProviderEnvironment environment() {
        return properties.getEnvironment();
    }

    private static void validateConfiguration(RazorpayBillingProperties properties) {
        if (properties == null) {
            throw new IllegalStateException("Razorpay billing properties are required");
        }
        if (properties.getEnvironment() == null) {
            throw new IllegalStateException("RAZORPAY_BILLING_ENVIRONMENT must be configured");
        }
        requireConfigured(properties.getKeyId(), "RAZORPAY_KEY_ID");
        requireConfigured(properties.getKeySecret(), "RAZORPAY_KEY_SECRET");
        requireConfigured(properties.getBaseUrl(), "RAZORPAY_BASE_URL");
    }

    private static void requireConfigured(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    environmentVariable + " must be configured for Razorpay billing");
        }
    }

    private record RazorpayPlanRequest(
            String period, int interval, RazorpayPlanItem item, Map<String, String> notes) {}

    private record RazorpayPlanItem(
            String name, long amount, String currency, String description) {}
}
