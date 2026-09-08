package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignment;
import java.time.Instant;
import java.util.UUID;

public record AuthorizationPermissionDecision(
        boolean granted,
        AuthorizationAccessDecisionReason reason,
        String normalizedPermissionCode,
        Instant evaluatedAt,
        UUID assignmentId,
        UUID roleId,
        String roleCode,
        AuthorizationScopeType scopeType,
        UUID scopeTargetId,
        Instant validFrom,
        Instant validUntil) {

    public static AuthorizationPermissionDecision granted(
            String normalizedPermissionCode,
            Instant evaluatedAt,
            AuthorizationUserRoleAssignment assignment) {
        return new AuthorizationPermissionDecision(
                true,
                AuthorizationAccessDecisionReason.GRANTED_BY_ROLE_ASSIGNMENT,
                normalizedPermissionCode,
                evaluatedAt,
                assignment.getId(),
                assignment.getRole().getId(),
                assignment.getRole().getCode(),
                assignment.getScopeType(),
                assignment.getScopeTargetId(),
                assignment.getValidFrom(),
                assignment.getValidUntil());
    }

    public static AuthorizationPermissionDecision denied(
            AuthorizationAccessDecisionReason reason,
            String normalizedPermissionCode,
            Instant evaluatedAt) {
        return new AuthorizationPermissionDecision(
                false,
                reason,
                normalizedPermissionCode,
                evaluatedAt,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
