package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record TenantSubscriptionStartRequest(
        @NotNull(message = "Plan id is required") UUID planId,
        @NotNull(message = "Subscription status is required") TenantSubscriptionStatus status,
        Instant startedAt,
        Instant currentPeriodStart,
        @NotNull(message = "Current period end is required") Instant currentPeriodEnd,
        Instant trialEndsAt,
        boolean cancelAtPeriodEnd) {}
