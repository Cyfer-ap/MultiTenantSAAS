package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserOrganizationAssignment;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.repository.UserOrganizationAssignmentRepository;
import com.chacha.multitenantsaas.service.AuthorizationPermissionEvaluator;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("authorizationSecurity")
public class AuthorizationSecurityService {

    private final AuthorizationPermissionEvaluator authorizationPermissionEvaluator;

    /*
     * ProjectSecurityService is retained only for exact
     * resource relationships:
     *
     * - project membership
     * - project-lead membership
     * - task assignment
     *
     * It is no longer used for legacy tenant-role fallback.
     */
    private final ProjectSecurityService projectSecurityService;

    private final UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;

    private final TenantRepository tenantRepository;

    public AuthorizationSecurityService(
            AuthorizationPermissionEvaluator authorizationPermissionEvaluator,
            ProjectSecurityService projectSecurityService,
            UserOrganizationAssignmentRepository userOrganizationAssignmentRepository,
            TenantRepository tenantRepository) {
        this.authorizationPermissionEvaluator = authorizationPermissionEvaluator;

        this.projectSecurityService = projectSecurityService;

        this.userOrganizationAssignmentRepository = userOrganizationAssignmentRepository;

        this.tenantRepository = tenantRepository;
    }

    /*
     * Core Authorization V2 evaluations
     */

    public boolean hasTenantPermission(UUID tenantId, String permissionCode) {
        return evaluate(tenantId, permissionCode, AuthorizationEvaluationContext.tenant());
    }

    public boolean hasUserPermission(UUID tenantId, UUID targetUserId, String permissionCode) {
        return evaluate(
                tenantId, permissionCode, AuthorizationEvaluationContext.user(targetUserId));
    }

    public boolean hasOrganizationalUnitPermission(
            UUID tenantId, UUID organizationalUnitId, String permissionCode) {
        return evaluate(
                tenantId,
                permissionCode,
                AuthorizationEvaluationContext.organizationalUnit(organizationalUnitId));
    }

    public boolean hasOrganizationalSubtreePermission(
            UUID tenantId, UUID organizationalUnitId, String permissionCode) {
        return evaluate(
                tenantId,
                permissionCode,
                AuthorizationEvaluationContext.organizationalSubtree(organizationalUnitId));
    }

    public boolean hasDirectReportsPermission(
            UUID tenantId, UUID managerAssignmentId, String permissionCode) {
        return evaluate(
                tenantId,
                permissionCode,
                AuthorizationEvaluationContext.directReports(managerAssignmentId));
    }

    public boolean hasProjectPermission(UUID tenantId, UUID projectId, String permissionCode) {
        return evaluate(
                tenantId, permissionCode, AuthorizationEvaluationContext.project(projectId));
    }

    public boolean hasTenantPermissionBySlug(String tenantSlug, String permissionCode) {
        if (tenantSlug == null || tenantSlug.trim().isEmpty()) {
            return false;
        }

        Tenant tenant = tenantRepository.findBySlug(tenantSlug.trim()).orElse(null);

        if (tenant == null) {
            return false;
        }

        return hasTenantPermission(tenant.getId(), permissionCode);
    }

    /*
     * Tenant dashboard
     */

    public boolean canReadCurrentTenantDashboard() {
        CurrentAuthorizationIdentity identity = getCurrentIdentity();

        if (identity == null) {
            return false;
        }

        UUID tenantId = identity.tenantId();

        return hasTenantPermission(tenantId, PlatformPermissionCodes.TENANT_READ)
                && hasTenantPermission(tenantId, PlatformPermissionCodes.USER_READ)
                && hasTenantPermission(tenantId, PlatformPermissionCodes.PROJECT_READ)
                && hasTenantPermission(tenantId, PlatformPermissionCodes.PROJECT_TASK_READ);
    }

    /*
     * Project task authorization
     *
     * A V2 permission can authorize the operation directly.
     * Exact project relationships remain valid independent
     * resource-level rules.
     */

    public boolean canReadProjectTasks(UUID tenantId, UUID projectId, String permissionCode) {
        return hasProjectPermission(tenantId, projectId, permissionCode)
                || projectSecurityService.isCurrentUserProjectMember(tenantId, projectId);
    }

    public boolean canManageProjectTasks(UUID tenantId, UUID projectId, String permissionCode) {
        return hasProjectPermission(tenantId, projectId, permissionCode)
                || projectSecurityService.isCurrentUserProjectLead(tenantId, projectId);
    }

    public boolean canUpdateProjectTaskStatus(
            UUID tenantId, UUID projectId, UUID taskId, String permissionCode) {
        return hasProjectPermission(tenantId, projectId, permissionCode)
                || projectSecurityService.isCurrentUserProjectLead(tenantId, projectId)
                || projectSecurityService.isCurrentUserTaskAssignee(tenantId, projectId, taskId);
    }

    /*
     * Organizational hierarchy authorization
     */

    public boolean canCreateOrganizationalUnit(
            UUID tenantId, UUID parentUnitId, String permissionCode) {
        if (parentUnitId == null) {
            return hasTenantPermission(tenantId, permissionCode);
        }

        return hasOrganizationalUnitPermission(tenantId, parentUnitId, permissionCode);
    }

    public boolean canMoveOrganizationalUnit(
            UUID tenantId, UUID sourceUnitId, UUID destinationParentUnitId, String permissionCode) {
        boolean canManageSource =
                hasOrganizationalUnitPermission(tenantId, sourceUnitId, permissionCode);

        if (!canManageSource) {
            return false;
        }

        if (destinationParentUnitId == null) {
            return hasTenantPermission(tenantId, permissionCode);
        }

        return hasOrganizationalUnitPermission(tenantId, destinationParentUnitId, permissionCode);
    }

    /*
     * Organizational assignment authorization
     */

    public boolean canCreateOrganizationAssignment(
            UUID tenantId, UUID organizationalUnitId, String permissionCode) {
        return hasOrganizationalUnitPermission(tenantId, organizationalUnitId, permissionCode);
    }

    public boolean canAccessOrganizationAssignment(
            UUID tenantId, UUID assignmentId, String permissionCode) {
        UserOrganizationAssignment assignment = getOrganizationAssignment(tenantId, assignmentId);

        if (assignment == null) {
            return false;
        }

        return hasUserPermission(tenantId, assignment.getUser().getId(), permissionCode)
                || hasOrganizationalUnitPermission(
                        tenantId, assignment.getOrganizationalUnit().getId(), permissionCode);
    }

    public boolean canReadDirectReportsAssignments(
            UUID tenantId, UUID managerAssignmentId, String permissionCode) {
        UserOrganizationAssignment managerAssignment =
                getOrganizationAssignment(tenantId, managerAssignmentId);

        if (managerAssignment == null) {
            return false;
        }

        return hasDirectReportsPermission(tenantId, managerAssignmentId, permissionCode)
                || hasOrganizationalUnitPermission(
                        tenantId,
                        managerAssignment.getOrganizationalUnit().getId(),
                        permissionCode);
    }

    private UserOrganizationAssignment getOrganizationAssignment(UUID tenantId, UUID assignmentId) {
        if (tenantId == null || assignmentId == null) {
            return null;
        }

        return userOrganizationAssignmentRepository
                .findByTenant_IdAndId(tenantId, assignmentId)
                .orElse(null);
    }

    private boolean evaluate(
            UUID routeTenantId, String permissionCode, AuthorizationEvaluationContext context) {
        try {
            CurrentAuthorizationIdentity identity = getCurrentIdentity();

            if (identity == null
                    || routeTenantId == null
                    || !routeTenantId.equals(identity.tenantId())) {
                return false;
            }

            return authorizationPermissionEvaluator.hasPermission(
                    routeTenantId, identity.userId(), permissionCode, context);

        } catch (RuntimeException exception) {
            return false;
        }
    }

    private CurrentAuthorizationIdentity getCurrentIdentity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        UUID tenantId = parseUuid(jwt.getClaimAsString("tenantId"));

        UUID userId = parseUuid(jwt.getSubject());

        if (tenantId == null || userId == null) {
            return null;
        }

        return new CurrentAuthorizationIdentity(tenantId, userId);
    }

    private UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);

        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record CurrentAuthorizationIdentity(UUID tenantId, UUID userId) {}

    public boolean isCurrentTenant(UUID tenantId) {
        try {
            CurrentAuthorizationIdentity identity = getCurrentIdentity();

            return identity != null && tenantId != null && tenantId.equals(identity.tenantId());

        } catch (RuntimeException exception) {
            return false;
        }
    }
}
