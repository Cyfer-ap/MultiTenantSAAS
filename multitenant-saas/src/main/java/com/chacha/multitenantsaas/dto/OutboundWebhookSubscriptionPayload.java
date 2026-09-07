package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record OutboundWebhookSubscriptionPayload(
        UUID subscriptionId,
        UUID tenantId,
        UUID planId,
        String planCode,
        String planName,
        TenantSubscriptionStatus status,
        BillingProviderType billingProvider,
        String providerSubscriptionId,
        Instant startedAt,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Instant trialEndsAt,
        boolean cancelAtPeriodEnd,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt) {

    public static OutboundWebhookSubscriptionPayload from(TenantSubscription subscription) {
        return new OutboundWebhookSubscriptionPayload(
                subscription.getId(),
                subscription.getTenant().getId(),
                subscription.getPlan().getId(),
                subscription.getPlanCodeSnapshot(),
                subscription.getPlanNameSnapshot(),
                subscription.getStatus(),
                subscription.getBillingProvider(),
                subscription.getProviderSubscriptionId(),
                subscription.getStartedAt(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getTrialEndsAt(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getCancelledAt(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt());
    }
}
