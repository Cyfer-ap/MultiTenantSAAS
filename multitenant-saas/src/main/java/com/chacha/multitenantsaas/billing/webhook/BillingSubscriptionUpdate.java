package com.chacha.multitenantsaas.billing.webhook;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record BillingSubscriptionUpdate(
        BillingProviderType provider,
        String providerSubscriptionId,
        UUID tenantId,
        String planCode,
        TenantSubscriptionStatus status,
        Instant startedAt,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Instant trialEndsAt,
        boolean cancelAtPeriodEnd,
        Instant occurredAt) {}
