package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record TenantSubscriptionResponse(
        UUID id,
        UUID tenantId,
        String tenantName,
        SubscriptionPlanResponse plan,
        TenantSubscriptionStatus status,
        Instant startedAt,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Instant trialEndsAt,
        boolean cancelAtPeriodEnd,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt) {}
