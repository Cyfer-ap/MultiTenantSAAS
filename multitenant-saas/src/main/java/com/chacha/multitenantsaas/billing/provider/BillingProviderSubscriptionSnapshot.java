package com.chacha.multitenantsaas.billing.provider;

import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;

public record BillingProviderSubscriptionSnapshot(
        BillingProviderType provider,
        String providerSubscriptionId,
        String planCode,
        TenantSubscriptionStatus status,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd) {}
