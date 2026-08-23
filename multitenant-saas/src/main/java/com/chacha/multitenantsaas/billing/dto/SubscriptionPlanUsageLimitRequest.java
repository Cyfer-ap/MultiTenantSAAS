package com.chacha.multitenantsaas.billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SubscriptionPlanUsageLimitRequest(
        @NotNull(message = "Period limit is required") @PositiveOrZero(message = "Period limit must not be negative") Long periodLimit) {}
