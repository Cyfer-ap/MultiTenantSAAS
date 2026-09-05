package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.provider.BillingCancellationResult;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.billing.provider.BillingProviderSubscriptionSnapshot;
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
     *
     * <p>If the persisted provider linkage is stale, a failed cancellation is followed by an
     * ownership check against the other configured billing providers. A provider is selected only
     * when its subscription lookup returns the same provider subscription ID; the repaired provider
     * linkage is then persisted before cancellation is retried through the verified owner.
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

        BillingProviderType recordedProviderType = subscription.getBillingProvider();
        String providerSubscriptionId = subscription.getProviderSubscriptionId();
        if (recordedProviderType == null
                || providerSubscriptionId == null
                || providerSubscriptionId.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant subscription is not linked to a billing provider");
        }

        BillingProvider recordedProvider = providerRegistry.require(recordedProviderType);
        try {
            recordedProvider.cancelSubscription(providerSubscriptionId);
            return new BillingCancellationResult(
                    tenantId, recordedProviderType, providerSubscriptionId);
        } catch (BillingProviderException recordedFailure) {
            ResolvedProvider verifiedOwner =
                    findAlternativeOwner(recordedProviderType, providerSubscriptionId);
            if (verifiedOwner == null) {
                throw recordedFailure;
            }

            subscription.setBillingProvider(verifiedOwner.type());
            tenantSubscriptionRepository.save(subscription);
            verifiedOwner.provider().cancelSubscription(providerSubscriptionId);

            return new BillingCancellationResult(
                    tenantId, verifiedOwner.type(), providerSubscriptionId);
        }
    }

    private ResolvedProvider findAlternativeOwner(
            BillingProviderType recordedProviderType, String providerSubscriptionId) {
        for (BillingProviderType candidateType : providerRegistry.availableProviderTypes()) {
            if (candidateType == recordedProviderType) {
                continue;
            }

            BillingProvider candidate = providerRegistry.require(candidateType);
            try {
                BillingProviderSubscriptionSnapshot snapshot =
                        candidate.fetchSubscription(providerSubscriptionId);
                if (snapshot != null
                        && snapshot.provider() == candidateType
                        && providerSubscriptionId.equals(snapshot.providerSubscriptionId())) {
                    return new ResolvedProvider(candidateType, candidate);
                }
            } catch (BillingProviderException ignored) {
                // This provider does not own the subscription, or it cannot currently verify it.
            }
        }
        return null;
    }

    private record ResolvedProvider(BillingProviderType type, BillingProvider provider) {}
}
