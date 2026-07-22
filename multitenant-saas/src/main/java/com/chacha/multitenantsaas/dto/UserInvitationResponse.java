package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserInvitationStatus;
import com.chacha.multitenantsaas.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserInvitationResponse(
        UUID invitationId,
        UUID tenantId,
        String fullName,
        String email,
        UserRole role,
        UserInvitationStatus status,
        Instant expiresAt,
        String devInvitationToken,
        String message
) {
}