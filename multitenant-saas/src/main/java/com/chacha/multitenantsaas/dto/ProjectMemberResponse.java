package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.ProjectMemberRole;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID membershipId,
        UUID projectId,
        UUID userId,
        String fullName,
        String email,
        UserRole tenantRole,
        UserStatus userStatus,
        ProjectMemberRole projectRole,
        UUID assignedByUserId,
        String assignedByUserName,
        String assignedByUserEmail,
        Instant assignedAt,
        Instant updatedAt
) {
}