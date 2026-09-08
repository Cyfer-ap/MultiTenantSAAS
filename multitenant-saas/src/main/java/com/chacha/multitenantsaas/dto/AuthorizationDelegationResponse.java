package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.AuthorizationDelegationStatus;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import java.time.Instant;
import java.util.UUID;

public record AuthorizationDelegationResponse(
        UUID id,
        UUID tenantId,
        UUID delegatorUserId,
        String delegatorEmail,
        UUID delegateUserId,
        String delegateEmail,
        UUID parentAssignmentId,
        UUID delegatedAssignmentId,
        UUID roleId,
        String roleCode,
        String roleName,
        AuthorizationScopeType scopeType,
        UUID scopeTargetId,
        AuthorizationDelegationStatus status,
        Instant validFrom,
        Instant validUntil,
        Instant createdAt,
        Instant revokedAt,
        UUID revokedByUserId,
        String revokedByEmail) {}
