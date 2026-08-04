package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record TenantSubscriptionLifecycleUpdateRequest(
        @NotNull(message = "Subscription status is required")
        TenantSubscriptionStatus status,

        boolean cancelAtPeriodEnd,

        Instant currentPeriodEnd,

        Instant trialEndsAt
) {
}
