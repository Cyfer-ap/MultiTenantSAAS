package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.BillingInterval;
import com.chacha.multitenantsaas.entity.TenantSubscriptionHistoryEventType;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TenantSubscriptionHistoryResponse(
        UUID id,
        UUID subscriptionId,
        UUID tenantId,
        String tenantName,
        UUID planId,
        String planCode,
        String planName,
        String planDescription,
        BillingInterval billingInterval,
        BigDecimal price,
        String currency,
        Integer maxUsers,
        Integer maxProjects,
        Long maxStorageMb,
        TenantSubscriptionStatus status,
        Instant startedAt,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Instant trialEndsAt,
        boolean cancelAtPeriodEnd,
        Instant cancelledAt,
        BillingProviderType billingProvider,
        String providerSubscriptionId,
        Instant providerEventCreatedAt,
        TenantSubscriptionHistoryEventType eventType,
        Instant recordedAt) {}
