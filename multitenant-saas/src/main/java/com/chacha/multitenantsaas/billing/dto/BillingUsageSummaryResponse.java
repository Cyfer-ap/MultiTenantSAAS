package com.chacha.multitenantsaas.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record BillingUsageSummaryResponse(
        UUID tenantId,
        String metricCode,
        Instant periodStart,
        Instant periodEnd,
        long quantity,
        long eventCount) {}
