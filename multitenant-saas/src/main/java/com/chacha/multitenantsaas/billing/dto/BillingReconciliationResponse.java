package com.chacha.multitenantsaas.billing.dto;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BillingReconciliationResponse(
        UUID tenantId,
        BillingProviderType provider,
        String providerSubscriptionId,
        String localPlanCode,
        String providerPlanCode,
        TenantSubscriptionStatus localStatus,
        TenantSubscriptionStatus providerStatus,
        Instant localCurrentPeriodStart,
        Instant providerCurrentPeriodStart,
        Instant localCurrentPeriodEnd,
        Instant providerCurrentPeriodEnd,
        boolean localCancelAtPeriodEnd,
        boolean providerCancelAtPeriodEnd,
        List<BillingReconciliationMismatch> mismatches,
        boolean consistent,
        Instant checkedAt) {}
