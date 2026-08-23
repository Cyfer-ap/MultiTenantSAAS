package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.dto.BillingCheckoutConfigurationResponse;
import com.chacha.multitenantsaas.billing.provider.BillingCheckoutSession;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import com.chacha.multitenantsaas.service.SubscriptionPlanService;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BillingCheckoutService {

    private final BillingProviderRegistry providerRegistry;
    private final SubscriptionPlanService subscriptionPlanService;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;

    public BillingCheckoutService(
            BillingProviderRegistry providerRegistry,
            SubscriptionPlanService subscriptionPlanService,
            TenantSubscriptionRepository tenantSubscriptionRepository) {
        this.providerRegistry = providerRegistry;
        this.subscriptionPlanService = subscriptionPlanService;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
    }

    public BillingCheckoutConfigurationResponse getCheckoutConfiguration() {
        List<SubscriptionPlanResponse> paidPlans =
                subscriptionPlanService.getPlans(true).stream()
                        .filter(plan -> plan.price().signum() > 0)
                        .toList();

        return new BillingCheckoutConfigurationResponse(
                paidPlans, providerRegistry.availableProviderTypes());
    }

    public BillingCheckoutSession createCheckoutSession(
            UUID tenantId, String planCode, BillingProviderType providerType) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(providerType, "providerType must not be null");
        if (planCode == null || planCode.isBlank()) {
            throw new IllegalArgumentException("planCode must not be blank");
        }

        SubscriptionPlanResponse plan = subscriptionPlanService.getPlanByCode(planCode);
        if (plan.status() != SubscriptionPlanStatus.ACTIVE) {
            throw new IllegalArgumentException("Subscription plan must be active");
        }
        if (plan.price().signum() <= 0) {
            throw new IllegalArgumentException("Free plans do not require provider checkout");
        }

        rejectDuplicateSubscriptionCheckout(tenantId);
        return providerRegistry.require(providerType).createCheckoutSession(tenantId, plan.code());
    }

    private void rejectDuplicateSubscriptionCheckout(UUID tenantId) {
        tenantSubscriptionRepository
                .findByTenantIdWithPlan(tenantId)
                .filter(subscription -> !isTerminal(subscription.getStatus()))
                .ifPresent(
                        subscription -> {
                            throw new IllegalArgumentException(
                                    "Workspace already has an active subscription");
                        });
    }

    private boolean isTerminal(TenantSubscriptionStatus status) {
        return status == TenantSubscriptionStatus.CANCELLED
                || status == TenantSubscriptionStatus.EXPIRED;
    }
}
