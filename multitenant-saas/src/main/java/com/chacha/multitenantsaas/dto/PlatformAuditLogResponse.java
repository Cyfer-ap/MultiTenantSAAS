package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.PlatformAuditAction;

import java.time.Instant;
import java.util.UUID;

public record PlatformAuditLogResponse(
        UUID id,
        UUID actorSystemAdminId,
        String actorSystemAdminEmail,
        UUID targetSystemAdminId,
        String targetSystemAdminEmail,
        PlatformAuditAction action,
        boolean success,
        String message,
        Instant createdAt
) {
}