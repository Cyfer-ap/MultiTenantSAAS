package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID tenantId,
        UUID recipientUserId,
        NotificationType type,
        String title,
        String body,
        String targetUrl,
        Instant readAt,
        Instant createdAt) {}
