package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.dto.BillingReconciliationMismatch;
import com.chacha.multitenantsaas.billing.dto.BillingReconciliationResponse;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.billing.provider.BillingProviderSubscriptionSnapshot;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BillingReconciliationService {

    private final BillingProviderRegistry providerRegistry;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;

    public BillingReconciliationService(
            BillingProviderRegistry providerRegistry,
            TenantSubscriptionRepository tenantSubscriptionRepository) {
        this.providerRegistry = providerRegistry;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
    }

    /**
     * Reads current state from the linked provider and reports differences. Local lifecycle state
     * is intentionally not mutated; verified webhooks remain the authoritative write path.
     */
    public BillingReconciliationResponse reconcile(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");

        TenantSubscription subscription =
                tenantSubscriptionRepository
                        .findByTenantIdWithPlan(tenantId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Tenant subscription not found for tenant: "
                                                        + tenantId));

        BillingProviderType providerType = subscription.getBillingProvider();
        String providerSubscriptionId = subscription.getProviderSubscriptionId();
        if (providerType == null
                || providerSubscriptionId == null
                || providerSubscriptionId.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant subscription is not linked to a billing provider");
        }

        BillingProvider provider = providerRegistry.require(providerType);
        BillingProviderSubscriptionSnapshot providerState =
                provider.fetchSubscription(providerSubscriptionId);
        if (providerState == null) {
            throw new BillingProviderException(
                    "Billing provider returned no subscription state", null);
        }

        List<BillingReconciliationMismatch> mismatches = new ArrayList<>();
        addMismatch(
                mismatches,
                providerType != providerState.provider(),
                BillingReconciliationMismatch.PROVIDER);
        addMismatch(
                mismatches,
                !providerSubscriptionId.equals(providerState.providerSubscriptionId()),
                BillingReconciliationMismatch.PROVIDER_SUBSCRIPTION_ID);
        addMismatch(
                mismatches,
                !samePlan(subscription.getPlan().getCode(), providerState.planCode()),
                BillingReconciliationMismatch.PLAN_CODE);
        addMismatch(
                mismatches,
                subscription.getStatus() != providerState.status(),
                BillingReconciliationMismatch.STATUS);
        addMismatch(
                mismatches,
                !Objects.equals(
                        subscription.getCurrentPeriodStart(), providerState.currentPeriodStart()),
                BillingReconciliationMismatch.CURRENT_PERIOD_START);
        addMismatch(
                mismatches,
                !Objects.equals(
                        subscription.getCurrentPeriodEnd(), providerState.currentPeriodEnd()),
                BillingReconciliationMismatch.CURRENT_PERIOD_END);
        addMismatch(
                mismatches,
                subscription.isCancelAtPeriodEnd() != providerState.cancelAtPeriodEnd(),
                BillingReconciliationMismatch.CANCEL_AT_PERIOD_END);

        List<BillingReconciliationMismatch> immutableMismatches = List.copyOf(mismatches);
        return new BillingReconciliationResponse(
                tenantId,
                providerType,
                providerSubscriptionId,
                subscription.getPlan().getCode(),
                providerState.planCode(),
                subscription.getStatus(),
                providerState.status(),
                subscription.getCurrentPeriodStart(),
                providerState.currentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                providerState.currentPeriodEnd(),
                subscription.isCancelAtPeriodEnd(),
                providerState.cancelAtPeriodEnd(),
                immutableMismatches,
                immutableMismatches.isEmpty(),
                Instant.now());
    }

    private boolean samePlan(String localPlanCode, String providerPlanCode) {
        return providerPlanCode != null && localPlanCode.equalsIgnoreCase(providerPlanCode);
    }

    private void addMismatch(
            List<BillingReconciliationMismatch> mismatches,
            boolean mismatch,
            BillingReconciliationMismatch type) {
        if (mismatch) {
            mismatches.add(type);
        }
    }
}
