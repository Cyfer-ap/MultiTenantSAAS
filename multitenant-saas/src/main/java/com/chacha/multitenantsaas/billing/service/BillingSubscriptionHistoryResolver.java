package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.entity.BillingEvent;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.repository.BillingEventRepository;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionEventMapper;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionUpdate;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BillingSubscriptionHistoryResolver {

    private final BillingEventRepository billingEventRepository;
    private final Map<BillingProviderType, BillingSubscriptionEventMapper> mappers;

    public BillingSubscriptionHistoryResolver(
            BillingEventRepository billingEventRepository,
            List<BillingSubscriptionEventMapper> mappers) {
        this.billingEventRepository = billingEventRepository;
        this.mappers = register(mappers);
    }

    /**
     * Returns the latest non-terminal verified subscription state for each provider subscription
     * that can be associated with the tenant from durable webhook history.
     */
    public List<BillingSubscriptionUpdate> findNonTerminalCandidates(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }

        Map<SubscriptionKey, BillingSubscriptionUpdate> latestBySubscription = new HashMap<>();
        List<BillingEvent> events =
                billingEventRepository.findTop200ByPayloadContainingOrderByReceivedAtDesc(
                        tenantId.toString());

        for (BillingEvent event : events) {
            map(event)
                    .filter(update -> tenantId.equals(update.tenantId()))
                    .ifPresent(
                            update -> {
                                SubscriptionKey key =
                                        new SubscriptionKey(
                                                update.provider(), update.providerSubscriptionId());
                                latestBySubscription.merge(
                                        key,
                                        update,
                                        (left, right) ->
                                                right.occurredAt().isAfter(left.occurredAt())
                                                        ? right
                                                        : left);
                            });
        }

        List<BillingSubscriptionUpdate> candidates = new ArrayList<>();
        for (BillingSubscriptionUpdate update : latestBySubscription.values()) {
            if (!isTerminal(update.status())) {
                candidates.add(update);
            }
        }
        candidates.sort(
                Comparator.comparing(BillingSubscriptionUpdate::occurredAt).reversed());
        return List.copyOf(candidates);
    }

    private Optional<BillingSubscriptionUpdate> map(BillingEvent event) {
        BillingSubscriptionEventMapper mapper = mappers.get(event.getProvider());
        if (mapper == null) {
            return Optional.empty();
        }

        try {
            return mapper.map(
                    new VerifiedBillingEvent(
                            event.getProvider(),
                            event.getProviderEventId(),
                            event.getEventType(),
                            event.getPayload()));
        } catch (IllegalArgumentException exception) {
            // Older verified payloads may no longer satisfy a stricter mapper revision. They are
            // ignored for repair rather than weakening current webhook validation.
            return Optional.empty();
        }
    }

    private boolean isTerminal(TenantSubscriptionStatus status) {
        return status == TenantSubscriptionStatus.CANCELLED
                || status == TenantSubscriptionStatus.EXPIRED;
    }

    private Map<BillingProviderType, BillingSubscriptionEventMapper> register(
            List<BillingSubscriptionEventMapper> candidates) {
        Map<BillingProviderType, BillingSubscriptionEventMapper> registered =
                new EnumMap<>(BillingProviderType.class);
        for (BillingSubscriptionEventMapper mapper : candidates) {
            BillingSubscriptionEventMapper previous =
                    registered.putIfAbsent(mapper.providerType(), mapper);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate billing subscription mapper: " + mapper.providerType());
            }
        }
        return Map.copyOf(registered);
    }

    private record SubscriptionKey(BillingProviderType provider, String providerSubscriptionId) {}
}
