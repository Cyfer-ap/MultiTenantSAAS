package com.chacha.multitenantsaas.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record BillingUsageRecordResponse(
        UUID eventId,
        UUID tenantId,
        String metricCode,
        long quantity,
        String idempotencyKey,
        Instant occurredAt,
        Instant recordedAt,
        boolean duplicate) {}
