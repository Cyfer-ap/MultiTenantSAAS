package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.service.AuthorizationPermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("authorizationSecurity")
public class AuthorizationSecurityService {

    private final AuthorizationPermissionEvaluator
            authorizationPermissionEvaluator;

    public AuthorizationSecurityService(
            AuthorizationPermissionEvaluator
                    authorizationPermissionEvaluator
    ) {
        this.authorizationPermissionEvaluator =
                authorizationPermissionEvaluator;
    }

    public boolean hasTenantPermission(
            UUID tenantId,
            String permissionCode
    ) {
        return evaluate(
                tenantId,
                permissionCode,
                AuthorizationEvaluationContext.tenant()
        );
    }

    public boolean hasUserPermission(
            UUID tenantId,
            UUID targetUserId,
            String permissionCode
    ) {
        return evaluate(
                tenantId,
                permissionCode,
                AuthorizationEvaluationContext.user(
                        targetUserId
                )
        );
    }

    public boolean hasOrganizationalUnitPermission(
            UUID tenantId,
            UUID organizationalUnitId,
            String permissionCode
    ) {
        return evaluate(
                tenantId,
                permissionCode,
                AuthorizationEvaluationContext
                        .organizationalUnit(
                                organizationalUnitId
                        )
        );
    }

    public boolean hasProjectPermission(
            UUID tenantId,
            UUID projectId,
            String permissionCode
    ) {
        return evaluate(
                tenantId,
                permissionCode,
                AuthorizationEvaluationContext.project(
                        projectId
                )
        );
    }

    private boolean evaluate(
            UUID routeTenantId,
            String permissionCode,
            AuthorizationEvaluationContext context
    ) {
        try {
            CurrentAuthorizationIdentity identity =
                    getCurrentIdentity();

            if (identity == null
                    || routeTenantId == null
                    || !routeTenantId.equals(
                    identity.tenantId()
            )) {
                return false;
            }

            return authorizationPermissionEvaluator
                    .hasPermission(
                            routeTenantId,
                            identity.userId(),
                            permissionCode,
                            context
                    );

        } catch (RuntimeException exception) {
            return false;
        }
    }

    private CurrentAuthorizationIdentity
    getCurrentIdentity() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof Jwt jwt)) {
            return null;
        }

        UUID tenantId =
                parseUuid(
                        jwt.getClaimAsString(
                                "tenantId"
                        )
                );

        UUID userId =
                parseUuid(
                        jwt.getSubject()
                );

        if (tenantId == null || userId == null) {
            return null;
        }

        return new CurrentAuthorizationIdentity(
                tenantId,
                userId
        );
    }

    private UUID parseUuid(String value) {
        try {
            return value == null
                    ? null
                    : UUID.fromString(value);

        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record CurrentAuthorizationIdentity(
            UUID tenantId,
            UUID userId
    ) {
    }
}