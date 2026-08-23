package com.chacha.multitenantsaas.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionPlanUsageLimitResponse(
        UUID id,
        UUID planId,
        String metricCode,
        long periodLimit,
        Instant createdAt,
        Instant updatedAt) {}
