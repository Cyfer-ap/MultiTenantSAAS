package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.BillingInterval;
import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionPlanResponse(
        UUID id,
        String code,
        String name,
        String description,
        BillingInterval billingInterval,
        BigDecimal price,
        String currency,
        Integer maxUsers,
        Integer maxProjects,
        Long maxStorageMb,
        SubscriptionPlanStatus status,
        Instant createdAt,
        Instant updatedAt) {}
