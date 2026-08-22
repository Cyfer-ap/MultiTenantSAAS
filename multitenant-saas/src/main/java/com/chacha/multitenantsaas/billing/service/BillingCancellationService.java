package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.provider.BillingCancellationResult;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BillingCancellationService {

    private final BillingProviderRegistry providerRegistry;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;

    public BillingCancellationService(
            BillingProviderRegistry providerRegistry,
            TenantSubscriptionRepository tenantSubscriptionRepository) {
        this.providerRegistry = providerRegistry;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
    }

    /**
     * Requests cancellation from the provider linked by a verified subscription webhook. The local
     * lifecycle state remains unchanged until provider webhook reconciliation completes.
     */
    public BillingCancellationResult requestCancellation(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");

        TenantSubscription subscription =
                tenantSubscriptionRepository
                        .findByTenantIdWithPlan(tenantId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Tenant subscription not found for tenant: "
                                                        + tenantId));

        if (subscription.getStatus() == TenantSubscriptionStatus.CANCELLED
                || subscription.getStatus() == TenantSubscriptionStatus.EXPIRED) {
            throw new IllegalArgumentException(
                    "Subscription is already terminal: " + subscription.getStatus());
        }

        BillingProviderType providerType = subscription.getBillingProvider();
        String providerSubscriptionId = subscription.getProviderSubscriptionId();
        if (providerType == null
                || providerSubscriptionId == null
                || providerSubscriptionId.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant subscription is not linked to a billing provider");
        }

        BillingProvider provider = providerRegistry.require(providerType);
        provider.cancelSubscription(providerSubscriptionId);

        return new BillingCancellationResult(tenantId, providerType, providerSubscriptionId);
    }
}
