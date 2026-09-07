package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import java.time.Instant;
import java.util.UUID;

public record OutboundWebhookDeliveryTask(
        UUID deliveryId,
        UUID leaseToken,
        UUID tenantId,
        UUID endpointId,
        UUID eventId,
        OutboundWebhookEventType eventType,
        String url,
        String secretCiphertext,
        int secretVersion,
        String payloadJson,
        Instant occurredAt) {}
