package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record OutboundWebhookEndpointResponse(
        UUID id,
        UUID tenantId,
        String name,
        String url,
        boolean enabled,
        Set<OutboundWebhookEventType> events,
        String secretHint,
        int secretVersion,
        UUID createdByUserId,
        UUID updatedByUserId,
        Instant secretRotatedAt,
        Instant createdAt,
        Instant updatedAt) {}
