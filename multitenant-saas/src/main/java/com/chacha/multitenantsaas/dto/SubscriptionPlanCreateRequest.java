package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.BillingInterval;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SubscriptionPlanCreateRequest(

        @NotBlank(message = "Plan code is required")
        @Size(max = 60, message = "Plan code must not exceed 60 characters")
        @Pattern(
                regexp = "[A-Za-z0-9_-]+",
                message = "Plan code may contain letters, numbers, underscores, and hyphens only"
        )
        String code,

        @NotBlank(message = "Plan name is required")
        @Size(max = 150, message = "Plan name must not exceed 150 characters")
        String name,

        @Size(max = 500, message = "Plan description must not exceed 500 characters")
        String description,

        @NotNull(message = "Billing interval is required")
        BillingInterval billingInterval,

        @NotNull(message = "Plan price is required")
        @DecimalMin(value = "0.00", message = "Plan price must not be negative")
        BigDecimal price,

        @NotBlank(message = "Currency is required")
        @Pattern(
                regexp = "[A-Za-z]{3}",
                message = "Currency must be a three-letter ISO code"
        )
        String currency,

        @PositiveOrZero(message = "Maximum users must not be negative")
        Integer maxUsers,

        @PositiveOrZero(message = "Maximum projects must not be negative")
        Integer maxProjects,

        @PositiveOrZero(message = "Maximum storage must not be negative")
        Long maxStorageMb
) {
}
