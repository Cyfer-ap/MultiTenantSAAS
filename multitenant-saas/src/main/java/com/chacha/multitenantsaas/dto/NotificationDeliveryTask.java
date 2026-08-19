package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationType;
import java.util.UUID;

public record NotificationDeliveryTask(
        UUID deliveryId,
        UUID leaseToken,
        UUID tenantId,
        UUID notificationId,
        UUID recipientUserId,
        String recipientEmail,
        NotificationDeliveryChannel channel,
        NotificationType type,
        String title,
        String body,
        String targetUrl) {}
