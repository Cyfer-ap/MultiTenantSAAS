package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record TenantSubscriptionPlanChangeRequest(
        @NotNull(message = "Plan id is required") UUID planId,
        @NotNull(message = "Current period start is required") Instant currentPeriodStart,
        @NotNull(message = "Current period end is required") Instant currentPeriodEnd) {}
