package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.AuthorizationRoleSource;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignmentStatus;
import java.time.Instant;
import java.util.UUID;

public record AuthorizationUserRoleAssignmentResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        String userFullName,
        String userEmail,
        UUID roleId,
        String roleCode,
        String roleName,
        AuthorizationRoleSource roleSource,
        AuthorizationScopeType scopeType,
        UUID scopeTargetId,
        AuthorizationUserRoleAssignmentStatus status,
        Instant validFrom,
        Instant validUntil,
        UUID createdByUserId,
        String createdByUserEmail,
        Instant createdAt,
        Instant updatedAt) {}
