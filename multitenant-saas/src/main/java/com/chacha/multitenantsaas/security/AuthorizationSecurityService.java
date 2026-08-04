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

    private final TenantSecurityService
            tenantSecurityService;

    private final ProjectSecurityService
            projectSecurityService;

    public AuthorizationSecurityService(
            AuthorizationPermissionEvaluator
                    authorizationPermissionEvaluator,
            TenantSecurityService tenantSecurityService,
            ProjectSecurityService projectSecurityService
    ) {
        this.authorizationPermissionEvaluator =
                authorizationPermissionEvaluator;

        this.tenantSecurityService =
                tenantSecurityService;

        this.projectSecurityService =
                projectSecurityService;
    }

    /*
     * Pure Authorization V2 checks
     */

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

    /*
     * Generic transitional fallback.
     *
     * Legacy access is used only when the current user has
     * never received an Authorization V2 assignment.
     */

    public boolean hasTenantPermissionOrLegacySameTenant(
            UUID tenantId,
            String permissionCode
    ) {
        if (hasTenantPermission(
                tenantId,
                permissionCode
        )) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isSameTenant(tenantId);
    }

    public boolean hasTenantPermissionOrLegacyAdmin(
            UUID tenantId,
            String permissionCode
    ) {
        if (hasTenantPermission(
                tenantId,
                permissionCode
        )) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isTenantAdmin(tenantId);
    }

    public boolean
    hasTenantPermissionOrLegacyAdminOrManager(
            UUID tenantId,
            String permissionCode
    ) {
        if (hasTenantPermission(
                tenantId,
                permissionCode
        )) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isTenantAdminOrManager(tenantId);
    }

    public boolean hasUserPermissionOrLegacyAdmin(
            UUID tenantId,
            UUID targetUserId,
            String permissionCode
    ) {
        if (hasUserPermission(
                tenantId,
                targetUserId,
                permissionCode
        )) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isTenantAdmin(tenantId);
    }

    public boolean
    hasUserPermissionOrLegacyAdminOrManager(
            UUID tenantId,
            UUID targetUserId,
            String permissionCode
    ) {
        if (hasUserPermission(
                tenantId,
                targetUserId,
                permissionCode
        )) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isTenantAdminOrManager(tenantId);
    }

    /*
     * Project endpoint compatibility
     */

    public boolean hasProjectPermissionOrLegacySameTenant(
            UUID tenantId,
            UUID projectId,
            String permissionCode
    ) {
        if (hasProjectPermission(
                tenantId,
                projectId,
                permissionCode
        )) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isSameTenant(tenantId);
    }

    public boolean
    hasProjectPermissionOrLegacyAdminOrManager(
            UUID tenantId,
            UUID projectId,
            String permissionCode
    ) {
        if (hasProjectPermission(
                tenantId,
                projectId,
                permissionCode
        )) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isTenantAdminOrManager(tenantId);
    }

    /*
     * Project task compatibility.
     *
     * Project membership, project-lead status and task
     * assignee status are themselves resource-scoped, so
     * they remain valid alongside V2 assignments.
     */

    public boolean hasProjectTaskPermissionOrLegacyRead(
            UUID tenantId,
            UUID projectId,
            String permissionCode
    ) {
        if (hasProjectPermission(
                tenantId,
                projectId,
                permissionCode
        )) {
            return true;
        }

        if (isLegacyFallbackAllowed(tenantId)) {
            return projectSecurityService
                    .canReadTasks(
                            tenantId,
                            projectId
                    );
        }

        return projectSecurityService
                .isCurrentUserProjectMember(
                        tenantId,
                        projectId
                );
    }

    public boolean hasProjectTaskPermissionOrLegacyManage(
            UUID tenantId,
            UUID projectId,
            String permissionCode
    ) {
        if (hasProjectPermission(
                tenantId,
                projectId,
                permissionCode
        )) {
            return true;
        }

        if (isLegacyFallbackAllowed(tenantId)) {
            return projectSecurityService
                    .canManageTasks(
                            tenantId,
                            projectId
                    );
        }

        return projectSecurityService
                .isCurrentUserProjectLead(
                        tenantId,
                        projectId
                );
    }

    public boolean
    hasProjectTaskStatusPermissionOrLegacy(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            String permissionCode
    ) {
        if (hasProjectPermission(
                tenantId,
                projectId,
                permissionCode
        )) {
            return true;
        }

        if (isLegacyFallbackAllowed(tenantId)) {
            return projectSecurityService
                    .canUpdateTaskStatus(
                            tenantId,
                            projectId,
                            taskId
                    );
        }

        return projectSecurityService
                .isCurrentUserProjectLead(
                        tenantId,
                        projectId
                )
                || projectSecurityService
                .isCurrentUserTaskAssignee(
                        tenantId,
                        projectId,
                        taskId
                );
    }

    private boolean isLegacyFallbackAllowed(
            UUID routeTenantId
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

            return !authorizationPermissionEvaluator
                    .hasAnyAuthorizationAssignment(
                            routeTenantId,
                            identity.userId()
                    );

        } catch (RuntimeException exception) {
            return false;
        }
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