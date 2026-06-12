package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        String userEmail,
        AuditAction action,
        boolean success,
        String message,
        Instant createdAt
) {
}