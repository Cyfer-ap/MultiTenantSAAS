package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.catalog.SubscriptionPlanCatalogService;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import com.chacha.multitenantsaas.service.SubscriptionPlanService;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionPlanRetirementCoordinator {

    private static final EnumSet<TenantSubscriptionStatus> CANCELLABLE_STATUSES =
            EnumSet.of(
                    TenantSubscriptionStatus.TRIALING,
                    TenantSubscriptionStatus.ACTIVE,
                    TenantSubscriptionStatus.PAST_DUE);

    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionPlanCatalogService catalogService;
    private final BillingProviderRegistry providerRegistry;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRetirementOperationService operationService;

    public SubscriptionPlanRetirementCoordinator(
            SubscriptionPlanService subscriptionPlanService,
            SubscriptionPlanCatalogService catalogService,
            BillingProviderRegistry providerRegistry,
            TenantSubscriptionRepository subscriptionRepository,
            SubscriptionPlanRetirementOperationService operationService) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.catalogService = catalogService;
        this.providerRegistry = providerRegistry;
        this.subscriptionRepository = subscriptionRepository;
        this.operationService = operationService;
    }

    public void execute(UUID planId) {
        operationService.markProcessing(planId);
        List<RuntimeException> failures = new ArrayList<>();
        SubscriptionPlanResponse plan = subscriptionPlanService.getPlan(planId);

        try {
            catalogService.planRetired(plan);
        } catch (RuntimeException ex) {
            failures.add(ex);
        }

        List<TenantSubscription> subscriptions =
                subscriptionRepository.findProviderLinkedSubscriptionsForPlan(
                        planId, CANCELLABLE_STATUSES);
        for (TenantSubscription subscription : subscriptions) {
            try {
                scheduleAtPeriodEnd(subscription);
            } catch (RuntimeException ex) {
                failures.add(ex);
            }
        }

        if (failures.isEmpty()) {
            operationService.markCompleted(planId);
            return;
        }

        IllegalStateException aggregate =
                new IllegalStateException(
                        "Plan is retired locally, but provider retirement cleanup is incomplete.",
                        failures.getFirst());
        failures.stream().skip(1).forEach(aggregate::addSuppressed);
        operationService.markFailed(planId, aggregate);
        throw aggregate;
    }

    private void scheduleAtPeriodEnd(TenantSubscription subscription) {
        BillingProvider provider = providerRegistry.require(subscription.getBillingProvider());
        String providerSubscriptionId = subscription.getProviderSubscriptionId();
        var snapshot = provider.fetchSubscription(providerSubscriptionId);
        if (snapshot.status() == TenantSubscriptionStatus.CANCELLED
                || snapshot.status() == TenantSubscriptionStatus.EXPIRED
                || snapshot.cancelAtPeriodEnd()) {
            return;
        }
        provider.scheduleCancellationAtPeriodEnd(providerSubscriptionId);
    }
}
