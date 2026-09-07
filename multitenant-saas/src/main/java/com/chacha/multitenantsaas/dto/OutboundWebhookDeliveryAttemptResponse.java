package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.OutboundWebhookDeliveryAttemptOutcome;
import java.time.Instant;
import java.util.UUID;

public record OutboundWebhookDeliveryAttemptResponse(
        UUID id,
        int replayNumber,
        int attemptNumber,
        OutboundWebhookDeliveryAttemptOutcome outcome,
        Integer httpStatus,
        String error,
        Instant startedAt,
        Instant completedAt) {}
