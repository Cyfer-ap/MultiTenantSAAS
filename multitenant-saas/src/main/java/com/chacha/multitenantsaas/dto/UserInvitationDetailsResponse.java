package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserInvitationStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserInvitationDetailsResponse(
        UUID invitationId,
        UUID tenantId,
        String fullName,
        String email,
        UserRole role,
        UserInvitationStatus status,
        boolean active,
        boolean expired,
        Instant expiresAt,
        Instant createdAt,
        Instant acceptedAt,
        Instant revokedAt,
        UUID invitedByUserId,
        String invitedByUserEmail,
        UUID invitedBySystemAdminId,
        String invitedBySystemAdminEmail) {}
