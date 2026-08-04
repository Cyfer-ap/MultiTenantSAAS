package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.service.AuthorizationPermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import com.chacha.multitenantsaas.entity.UserOrganizationAssignment;
import com.chacha.multitenantsaas.repository.UserOrganizationAssignmentRepository;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.util.UUID;

@Component("authorizationSecurity")
public class AuthorizationSecurityService {

    private final AuthorizationPermissionEvaluator
            authorizationPermissionEvaluator;

    private final TenantSecurityService
            tenantSecurityService;

    private final ProjectSecurityService
            projectSecurityService;

    private final UserOrganizationAssignmentRepository
            userOrganizationAssignmentRepository;
    private final TenantRepository tenantRepository;

    public AuthorizationSecurityService(
            AuthorizationPermissionEvaluator
                    authorizationPermissionEvaluator,
            TenantSecurityService tenantSecurityService,
            ProjectSecurityService projectSecurityService,
            UserOrganizationAssignmentRepository
                    userOrganizationAssignmentRepository,
            TenantRepository tenantRepository
    ) {
        this.authorizationPermissionEvaluator =
                authorizationPermissionEvaluator;

        this.tenantSecurityService =
                tenantSecurityService;

        this.projectSecurityService =
                projectSecurityService;

        this.userOrganizationAssignmentRepository =
                userOrganizationAssignmentRepository;

        this.tenantRepository = tenantRepository;
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

    public boolean hasOrganizationalSubtreePermission(
            UUID tenantId,
            UUID organizationalUnitId,
            String permissionCode
    ) {
        return evaluate(
                tenantId,
                permissionCode,
                AuthorizationEvaluationContext
                        .organizationalSubtree(
                                organizationalUnitId
                        )
        );
    }

    public boolean hasDirectReportsPermission(
            UUID tenantId,
            UUID managerAssignmentId,
            String permissionCode
    ) {
        return evaluate(
                tenantId,
                permissionCode,
                AuthorizationEvaluationContext
                        .directReports(
                                managerAssignmentId
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

    public boolean
    hasTenantPermissionBySlugOrLegacySameTenant(
            String tenantSlug,
            String permissionCode
    ) {
        if (tenantSlug == null
                || tenantSlug.trim().isEmpty()) {
            return false;
        }

        Tenant tenant =
                tenantRepository
                        .findBySlug(
                                tenantSlug.trim()
                        )
                        .orElse(null);

        if (tenant == null) {
            return false;
        }

        return hasTenantPermissionOrLegacySameTenant(
                tenant.getId(),
                permissionCode
        );
    }

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

    public boolean canReadCurrentTenantDashboard() {
        CurrentAuthorizationIdentity identity =
                getCurrentIdentity();

        if (identity == null) {
            return false;
        }

        UUID tenantId =
                identity.tenantId();

        boolean hasV2DashboardAccess =
                hasTenantPermission(
                        tenantId,
                        PlatformPermissionCodes.TENANT_READ
                )
                        && hasTenantPermission(
                        tenantId,
                        PlatformPermissionCodes.USER_READ
                )
                        && hasTenantPermission(
                        tenantId,
                        PlatformPermissionCodes.PROJECT_READ
                )
                        && hasTenantPermission(
                        tenantId,
                        PlatformPermissionCodes
                                .PROJECT_TASK_READ
                );

        if (hasV2DashboardAccess) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isCurrentTenantAdminOrManager();
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

    public boolean
    hasOrganizationalUnitPermissionOrLegacyAdmin(
            UUID tenantId,
            UUID organizationalUnitId,
            String permissionCode
    ) {
        if (hasOrganizationalUnitPermission(
                tenantId,
                organizationalUnitId,
                permissionCode
        )) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isTenantAdmin(tenantId);
    }

    public boolean
    hasOrganizationalSubtreePermissionOrLegacyAdmin(
            UUID tenantId,
            UUID organizationalUnitId,
            String permissionCode
    ) {
        if (hasOrganizationalSubtreePermission(
                tenantId,
                organizationalUnitId,
                permissionCode
        )) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isTenantAdmin(tenantId);
    }

    public boolean
    hasCreateOrganizationalUnitPermissionOrLegacyAdmin(
            UUID tenantId,
            UUID parentUnitId,
            String permissionCode
    ) {
        boolean authorized =
                parentUnitId == null
                        ? hasTenantPermission(
                        tenantId,
                        permissionCode
                )
                        : hasOrganizationalUnitPermission(
                        tenantId,
                        parentUnitId,
                        permissionCode
                );

        if (authorized) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isTenantAdmin(tenantId);
    }

    public boolean
    hasMoveOrganizationalUnitPermissionOrLegacyAdmin(
            UUID tenantId,
            UUID sourceUnitId,
            UUID destinationParentUnitId,
            String permissionCode
    ) {
        boolean canManageSource =
                hasOrganizationalUnitPermission(
                        tenantId,
                        sourceUnitId,
                        permissionCode
                );

        boolean canManageDestination =
                destinationParentUnitId == null
                        ? hasTenantPermission(
                        tenantId,
                        permissionCode
                )
                        : hasOrganizationalUnitPermission(
                        tenantId,
                        destinationParentUnitId,
                        permissionCode
                );

        if (canManageSource && canManageDestination) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isTenantAdmin(tenantId);
    }

    public boolean
    hasCreateOrganizationAssignmentPermissionOrLegacyAdmin(
            UUID tenantId,
            UUID organizationalUnitId,
            String permissionCode
    ) {
        if (hasOrganizationalUnitPermission(
                tenantId,
                organizationalUnitId,
                permissionCode
        )) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isTenantAdmin(tenantId);
    }

    public boolean
    hasUserOrganizationAssignmentPermissionOrLegacyAdmin(
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
    hasOrganizationAssignmentPermissionOrLegacyAdmin(
            UUID tenantId,
            UUID assignmentId,
            String permissionCode
    ) {
        UserOrganizationAssignment assignment =
                getOrganizationAssignment(
                        tenantId,
                        assignmentId
                );

        if (assignment == null) {
            return false;
        }

        boolean authorized =
                hasUserPermission(
                        tenantId,
                        assignment.getUser().getId(),
                        permissionCode
                )
                        || hasOrganizationalUnitPermission(
                        tenantId,
                        assignment
                                .getOrganizationalUnit()
                                .getId(),
                        permissionCode
                );

        if (authorized) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isTenantAdmin(tenantId);
    }

    public boolean
    hasDirectReportsOrganizationAssignmentPermissionOrLegacyAdmin(
            UUID tenantId,
            UUID managerAssignmentId,
            String permissionCode
    ) {
        UserOrganizationAssignment managerAssignment =
                getOrganizationAssignment(
                        tenantId,
                        managerAssignmentId
                );

        if (managerAssignment == null) {
            return false;
        }

        boolean authorized =
                hasDirectReportsPermission(
                        tenantId,
                        managerAssignmentId,
                        permissionCode
                )
                        || hasOrganizationalUnitPermission(
                        tenantId,
                        managerAssignment
                                .getOrganizationalUnit()
                                .getId(),
                        permissionCode
                );

        if (authorized) {
            return true;
        }

        return isLegacyFallbackAllowed(tenantId)
                && tenantSecurityService
                .isTenantAdmin(tenantId);
    }

    private UserOrganizationAssignment
    getOrganizationAssignment(
            UUID tenantId,
            UUID assignmentId
    ) {
        if (tenantId == null || assignmentId == null) {
            return null;
        }

        return userOrganizationAssignmentRepository
                .findByTenant_IdAndId(
                        tenantId,
                        assignmentId
                )
                .orElse(null);
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