package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID tenantId,
        UUID actorUserId,
        String actorUserEmail,
        UUID targetUserId,
        String targetUserEmail,
        AuditAction action,
        boolean success,
        String message,
        Instant createdAt
) {
}