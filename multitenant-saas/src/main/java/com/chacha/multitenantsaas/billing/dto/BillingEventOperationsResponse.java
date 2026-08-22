package com.chacha.multitenantsaas.billing.dto;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.time.Instant;
import java.util.UUID;

public record BillingEventOperationsResponse(
        UUID id,
        BillingProviderType provider,
        String providerEventId,
        String eventType,
        Instant receivedAt) {}
