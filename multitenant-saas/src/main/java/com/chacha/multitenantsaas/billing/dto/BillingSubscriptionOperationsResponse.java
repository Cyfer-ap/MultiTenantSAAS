package com.chacha.multitenantsaas.billing.dto;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record BillingSubscriptionOperationsResponse(
        UUID subscriptionId,
        UUID tenantId,
        String tenantName,
        String planCode,
        TenantSubscriptionStatus status,
        BillingProviderType provider,
        String providerSubscriptionId,
        Instant providerEventCreatedAt,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Instant updatedAt) {}
