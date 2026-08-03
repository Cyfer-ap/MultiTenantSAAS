package com.chacha.multitenantsaas.security;

import java.util.UUID;

public record AuthorizationEvaluationContext(

        UUID targetUserId,

        UUID organizationalUnitId,

        UUID projectId
) {

    public static AuthorizationEvaluationContext tenant() {
        return new AuthorizationEvaluationContext(
                null,
                null,
                null
        );
    }

    public static AuthorizationEvaluationContext user(
            UUID targetUserId
    ) {
        return new AuthorizationEvaluationContext(
                targetUserId,
                null,
                null
        );
    }

    public static AuthorizationEvaluationContext
    organizationalUnit(
            UUID organizationalUnitId
    ) {
        return new AuthorizationEvaluationContext(
                null,
                organizationalUnitId,
                null
        );
    }

    public static AuthorizationEvaluationContext project(
            UUID projectId
    ) {
        return new AuthorizationEvaluationContext(
                null,
                null,
                projectId
        );
    }
}