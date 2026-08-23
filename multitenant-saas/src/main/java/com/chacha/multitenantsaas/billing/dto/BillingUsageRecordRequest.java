package com.chacha.multitenantsaas.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record BillingUsageRecordRequest(
        @NotNull UUID tenantId,
        @NotBlank
                @Pattern(
                        regexp = "[A-Za-z][A-Za-z0-9_]{0,63}",
                        message = "metricCode must contain only letters, numbers, and underscores")
                String metricCode,
        @NotNull @Positive Long quantity,
        @NotBlank @Size(max = 128) String idempotencyKey,
        @NotNull Instant occurredAt) {}
