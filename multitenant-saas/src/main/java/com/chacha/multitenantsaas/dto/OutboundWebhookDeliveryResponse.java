package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.OutboundWebhookDeliveryStatus;
import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import java.time.Instant;
import java.util.UUID;

public record OutboundWebhookDeliveryResponse(
        UUID id,
        UUID endpointId,
        String endpointName,
        String endpointUrl,
        UUID eventId,
        OutboundWebhookEventType eventType,
        Instant eventOccurredAt,
        OutboundWebhookDeliveryStatus status,
        int attemptCount,
        int replayCount,
        Instant nextAttemptAt,
        Integer lastHttpStatus,
        String lastError,
        Instant sentAt,
        Instant createdAt,
        Instant updatedAt) {}
