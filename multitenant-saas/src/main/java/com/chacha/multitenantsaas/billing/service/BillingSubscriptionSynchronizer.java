package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionEventMapper;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionUpdate;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import com.chacha.multitenantsaas.service.SubscriptionPlanService;
import com.chacha.multitenantsaas.service.TenantLookupService;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class BillingSubscriptionSynchronizer {

    private final Map<BillingProviderType, BillingSubscriptionEventMapper> mappers;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantLookupService tenantLookupService;
    private final SubscriptionPlanService subscriptionPlanService;

    public BillingSubscriptionSynchronizer(
            List<BillingSubscriptionEventMapper> mappers,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            TenantLookupService tenantLookupService,
            SubscriptionPlanService subscriptionPlanService) {
        this.mappers = register(mappers);
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.tenantLookupService = tenantLookupService;
        this.subscriptionPlanService = subscriptionPlanService;
    }

    public void synchronize(VerifiedBillingEvent event) {
        BillingSubscriptionEventMapper mapper = mappers.get(event.provider());
        if (mapper == null) {
            return;
        }
        mapper.map(event).ifPresent(this::apply);
    }

    private void apply(BillingSubscriptionUpdate update) {
        TenantSubscription subscription =
                tenantSubscriptionRepository
                        .findByTenantIdWithPlanForUpdate(update.tenantId())
                        .orElse(null);
        if (subscription != null && isStaleOrTerminalRegression(subscription, update)) {
            return;
        }

        SubscriptionPlan plan =
                subscriptionPlanService.getActivePlanEntityByCode(update.planCode());
        if (subscription == null) {
            Tenant tenant = tenantLookupService.getActiveByIdForUpdateOrThrow(update.tenantId());
            subscription =
                    new TenantSubscription(
                            tenant,
                            plan,
                            update.status(),
                            update.startedAt(),
                            update.currentPeriodStart(),
                            update.currentPeriodEnd(),
                            update.trialEndsAt(),
                            update.cancelAtPeriodEnd());
        } else {
            subscription.setPlan(plan);
            subscription.setStatus(update.status());
            subscription.setCurrentPeriodStart(update.currentPeriodStart());
            subscription.setCurrentPeriodEnd(update.currentPeriodEnd());
            subscription.setTrialEndsAt(update.trialEndsAt());
            subscription.setCancelAtPeriodEnd(update.cancelAtPeriodEnd());
        }

        subscription.setBillingProvider(update.provider());
        subscription.setProviderSubscriptionId(update.providerSubscriptionId());
        subscription.setProviderEventCreatedAt(update.occurredAt());
        updateCancellation(subscription, update.status(), update.occurredAt());
        tenantSubscriptionRepository.save(subscription);
    }

    private boolean isStaleOrTerminalRegression(
            TenantSubscription subscription, BillingSubscriptionUpdate update) {
        if (subscription.getProviderEventCreatedAt() != null
                && update.occurredAt().isBefore(subscription.getProviderEventCreatedAt())) {
            return true;
        }

        boolean sameProviderSubscription =
                subscription.getBillingProvider() == update.provider()
                        && Objects.equals(
                                subscription.getProviderSubscriptionId(),
                                update.providerSubscriptionId());
        return sameProviderSubscription
                && isTerminal(subscription.getStatus())
                && !isTerminal(update.status());
    }

    private boolean isTerminal(TenantSubscriptionStatus status) {
        return status == TenantSubscriptionStatus.CANCELLED
                || status == TenantSubscriptionStatus.EXPIRED;
    }

    private void updateCancellation(
            TenantSubscription subscription,
            TenantSubscriptionStatus status,
            java.time.Instant occurredAt) {
        if (isTerminal(status)) {
            subscription.setCancelledAt(occurredAt);
            subscription.setCancelAtPeriodEnd(false);
        } else if (status == TenantSubscriptionStatus.ACTIVE
                || status == TenantSubscriptionStatus.TRIALING) {
            subscription.setCancelledAt(null);
        }
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
}
