package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.NotificationType;

public record NotificationPreferenceResponse(
        NotificationType type,
        boolean inAppEnabled,
        boolean emailEnabled,
        boolean emailConfigurable) {}
