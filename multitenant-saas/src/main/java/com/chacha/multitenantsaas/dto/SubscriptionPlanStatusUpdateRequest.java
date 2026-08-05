package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import jakarta.validation.constraints.NotNull;

public record SubscriptionPlanStatusUpdateRequest(

        @NotNull(message = "Subscription plan status is required")
        SubscriptionPlanStatus status
) {
}
