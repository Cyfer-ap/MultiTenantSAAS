package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.AuditActorType;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID tenantId,
        AuditActorType actorType,
        UUID actorUserId,
        String actorUserEmail,
        UUID actorSystemAdminId,
        String actorSystemAdminEmail,
        UUID targetUserId,
        String targetUserEmail,
        AuditAction action,
        boolean success,
        String message,
        Instant createdAt
) {
}