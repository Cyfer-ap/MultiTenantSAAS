package com.chacha.multitenantsaas.security;

import java.util.UUID;

public record AuthorizationEvaluationContext(
        UUID targetUserId,
        UUID organizationalUnitId,
        UUID projectId,
        UUID directReportsManagerAssignmentId,
        boolean requireSubtreeScope) {

    public static AuthorizationEvaluationContext tenant() {
        return new AuthorizationEvaluationContext(null, null, null, null, false);
    }

    public static AuthorizationEvaluationContext user(UUID targetUserId) {
        return new AuthorizationEvaluationContext(targetUserId, null, null, null, false);
    }

    public static AuthorizationEvaluationContext organizationalUnit(UUID organizationalUnitId) {
        return new AuthorizationEvaluationContext(null, organizationalUnitId, null, null, false);
    }

    public static AuthorizationEvaluationContext organizationalSubtree(UUID organizationalUnitId) {
        return new AuthorizationEvaluationContext(null, organizationalUnitId, null, null, true);
    }

    public static AuthorizationEvaluationContext project(UUID projectId) {
        return new AuthorizationEvaluationContext(null, null, projectId, null, false);
    }

    public static AuthorizationEvaluationContext directReports(UUID managerAssignmentId) {
        return new AuthorizationEvaluationContext(null, null, null, managerAssignmentId, false);
    }
}
