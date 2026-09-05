package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.provider.BillingCancellationResult;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionUpdate;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BillingCancellationService {

    private final BillingProviderRegistry providerRegistry;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final BillingSubscriptionHistoryResolver historyResolver;

    public BillingCancellationService(
            BillingProviderRegistry providerRegistry,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            BillingSubscriptionHistoryResolver historyResolver) {
        this.providerRegistry = providerRegistry;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.historyResolver = historyResolver;
    }

    /**
     * Requests cancellation from the provider linked by a verified subscription webhook. The local
     * lifecycle state remains unchanged until provider webhook reconciliation completes.
     *
     * <p>If the persisted provider linkage is stale, ownership is checked against the other
     * configured providers without requiring a complete provider snapshot. If the stored provider
     * subscription ID is itself stale, durable verified webhook history is used to recover a recent
     * non-terminal provider/subscription pair for the same tenant. A recovered linkage is persisted
     * only after the provider confirms ownership of that subscription ID.
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
            ResolvedSubscription verifiedOwner =
                    findAlternativeOwner(recordedProviderType, providerSubscriptionId);
            if (verifiedOwner == null) {
                verifiedOwner =
                        findHistoricalOwner(
                                tenantId,
                                subscription,
                                recordedProviderType,
                                providerSubscriptionId);
            }
            if (verifiedOwner == null) {
                throw recordedFailure;
            }

            repairLinkage(subscription, verifiedOwner);
            verifiedOwner.provider().cancelSubscription(verifiedOwner.providerSubscriptionId());

            return new BillingCancellationResult(
                    tenantId, verifiedOwner.type(), verifiedOwner.providerSubscriptionId());
        }
    }

    private ResolvedSubscription findAlternativeOwner(
            BillingProviderType recordedProviderType, String providerSubscriptionId) {
        for (BillingProviderType candidateType : providerRegistry.availableProviderTypes()) {
            if (candidateType == recordedProviderType) {
                continue;
            }

            BillingProvider candidate = providerRegistry.require(candidateType);
            if (providerOwns(candidate, providerSubscriptionId)) {
                return new ResolvedSubscription(
                        candidateType, candidate, providerSubscriptionId);
            }
        }
        return null;
    }

    private ResolvedSubscription findHistoricalOwner(
            UUID tenantId,
            TenantSubscription subscription,
            BillingProviderType recordedProviderType,
            String recordedSubscriptionId) {
        List<BillingSubscriptionUpdate> candidates =
                historyResolver.findNonTerminalCandidates(tenantId).stream()
                        .filter(
                                update ->
                                        update.provider() != recordedProviderType
                                                || !recordedSubscriptionId.equals(
                                                        update.providerSubscriptionId()))
                        .sorted(
                                Comparator.comparingInt(
                                                (BillingSubscriptionUpdate update) ->
                                                        historyMatchScore(subscription, update))
                                        .reversed()
                                        .thenComparing(
                                                BillingSubscriptionUpdate::occurredAt,
                                                Comparator.reverseOrder()))
                        .toList();

        for (BillingSubscriptionUpdate update : candidates) {
            BillingProvider provider;
            try {
                provider = providerRegistry.require(update.provider());
            } catch (IllegalArgumentException notConfigured) {
                continue;
            }

            if (providerOwns(provider, update.providerSubscriptionId())) {
                return new ResolvedSubscription(
                        update.provider(), provider, update.providerSubscriptionId());
            }
        }
        return null;
    }

    private int historyMatchScore(
            TenantSubscription subscription, BillingSubscriptionUpdate update) {
        int score = 0;
        SubscriptionPlan plan = subscription.getPlan();
        if (plan != null
                && plan.getCode() != null
                && update.planCode() != null
                && plan.getCode().equalsIgnoreCase(update.planCode())) {
            score += 8;
        }
        if (subscription.getStatus() == update.status()) {
            score += 4;
        }
        if (Objects.equals(
                subscription.getCurrentPeriodStart(), update.currentPeriodStart())) {
            score += 2;
        }
        if (Objects.equals(subscription.getCurrentPeriodEnd(), update.currentPeriodEnd())) {
            score += 2;
        }
        return score;
    }

    private boolean providerOwns(BillingProvider provider, String providerSubscriptionId) {
        try {
            return provider.ownsSubscription(providerSubscriptionId);
        } catch (BillingProviderException | IllegalArgumentException lookupFailure) {
            return false;
        }
    }

    private void repairLinkage(
            TenantSubscription subscription, ResolvedSubscription verifiedOwner) {
        subscription.setBillingProvider(verifiedOwner.type());
        subscription.setProviderSubscriptionId(verifiedOwner.providerSubscriptionId());
        tenantSubscriptionRepository.save(subscription);
    }

    private record ResolvedSubscription(
            BillingProviderType type,
            BillingProvider provider,
            String providerSubscriptionId) {}
}
