package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionStatus;
import com.chacha.multitenantsaas.entity.AuthorizationRoleStatus;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignment;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignmentStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationUserRoleAssignmentRepository;
import com.chacha.multitenantsaas.repository.OrganizationalUnitRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.AuthorizationEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthorizationPermissionEvaluator {

    private final TenantRepository tenantRepository;

    private final AppUserRepository appUserRepository;

    private final AuthorizationUserRoleAssignmentRepository
            assignmentRepository;

    private final OrganizationalUnitRepository
            organizationalUnitRepository;

    private final ProjectRepository projectRepository;

    private final AuthorizationScopeQueryService
            authorizationScopeQueryService;

    public AuthorizationPermissionEvaluator(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            AuthorizationUserRoleAssignmentRepository
                    assignmentRepository,
            OrganizationalUnitRepository
                    organizationalUnitRepository,
            ProjectRepository projectRepository,
            AuthorizationScopeQueryService
                    authorizationScopeQueryService
    ) {
        this.tenantRepository =
                tenantRepository;

        this.appUserRepository =
                appUserRepository;

        this.assignmentRepository =
                assignmentRepository;

        this.organizationalUnitRepository =
                organizationalUnitRepository;

        this.projectRepository =
                projectRepository;

        this.authorizationScopeQueryService =
                authorizationScopeQueryService;
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(
            UUID tenantId,
            UUID userId,
            String permissionCode,
            AuthorizationEvaluationContext context
    ) {
        return hasPermission(
                tenantId,
                userId,
                permissionCode,
                context,
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(
            UUID tenantId,
            UUID userId,
            String permissionCode,
            AuthorizationEvaluationContext context,
            Instant effectiveAt
    ) {
        if (tenantId == null
                || userId == null
                || permissionCode == null
                || effectiveAt == null) {
            return false;
        }

        String normalizedPermissionCode =
                normalizePermissionCode(
                        permissionCode
                );

        if (normalizedPermissionCode == null) {
            return false;
        }

        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElse(null);

        if (tenant == null
                || tenant.getStatus()
                != TenantStatus.ACTIVE) {
            return false;
        }

        AppUser user =
                appUserRepository
                        .findByTenantIdAndId(
                                tenantId,
                                userId
                        )
                        .orElse(null);

        if (user == null
                || user.getStatus()
                != UserStatus.ACTIVE) {
            return false;
        }

        Instant normalizedEffectiveAt =
                effectiveAt.truncatedTo(
                        ChronoUnit.MICROS
                );

        AuthorizationEvaluationContext
                resolvedContext =
                context == null
                        ? AuthorizationEvaluationContext
                        .tenant()
                        : context;

        List<AuthorizationUserRoleAssignment>
                effectiveGrants =
                assignmentRepository
                        .findEffectiveAssignmentsGrantingPermission(
                                tenantId,
                                userId,
                                normalizedPermissionCode,
                                AuthorizationUserRoleAssignmentStatus.ACTIVE,
                                AuthorizationRoleStatus.ACTIVE,
                                AuthorizationPermissionStatus.ACTIVE,
                                AuthorizationPermissionSource.PLATFORM,
                                normalizedEffectiveAt
                        );

        for (AuthorizationUserRoleAssignment grant
                : effectiveGrants) {
            if (matchesScope(
                    tenantId,
                    userId,
                    grant,
                    resolvedContext,
                    normalizedEffectiveAt
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesScope(
            UUID tenantId,
            UUID userId,
            AuthorizationUserRoleAssignment grant,
            AuthorizationEvaluationContext context,
            Instant effectiveAt
    ) {
        AuthorizationScopeType scopeType =
                grant.getScopeType();

        if (scopeType == null) {
            return false;
        }

        return switch (scopeType) {
            case TENANT -> true;

            case SELF ->
                    context.targetUserId() != null
                            && context
                            .targetUserId()
                            .equals(userId);

            case PROJECT ->
                    matchesProjectScope(
                            tenantId,
                            grant.getScopeTargetId(),
                            context.projectId()
                    );

            case ORGANIZATIONAL_UNIT ->
                    matchesExactUnitScope(
                            tenantId,
                            grant.getScopeTargetId(),
                            context.organizationalUnitId()
                    );

            case ORGANIZATIONAL_SUBTREE ->
                    matchesSubtreeScope(
                            tenantId,
                            grant.getScopeTargetId(),
                            context.organizationalUnitId()
                    );

            case DIRECT_REPORTS ->
                    matchesDirectReportsScope(
                            tenantId,
                            userId,
                            grant.getScopeTargetId(),
                            context.targetUserId(),
                            effectiveAt
                    );
        };
    }

    private boolean matchesProjectScope(
            UUID tenantId,
            UUID grantedProjectId,
            UUID requestedProjectId
    ) {
        if (grantedProjectId == null
                || requestedProjectId == null
                || !grantedProjectId.equals(
                requestedProjectId
        )) {
            return false;
        }

        Project project =
                projectRepository
                        .findByTenant_IdAndId(
                                tenantId,
                                requestedProjectId
                        )
                        .orElse(null);

        return project != null
                && project.getStatus()
                != ProjectStatus.ARCHIVED;
    }

    private boolean matchesExactUnitScope(
            UUID tenantId,
            UUID grantedUnitId,
            UUID requestedUnitId
    ) {
        if (grantedUnitId == null
                || requestedUnitId == null
                || !grantedUnitId.equals(
                requestedUnitId
        )) {
            return false;
        }

        return isActiveUnit(
                tenantId,
                requestedUnitId
        );
    }

    private boolean matchesSubtreeScope(
            UUID tenantId,
            UUID grantedRootUnitId,
            UUID requestedUnitId
    ) {
        if (grantedRootUnitId == null
                || requestedUnitId == null) {
            return false;
        }

        if (!isActiveUnit(
                tenantId,
                grantedRootUnitId
        )) {
            return false;
        }

        if (!isActiveUnit(
                tenantId,
                requestedUnitId
        )) {
            return false;
        }

        return authorizationScopeQueryService
                .isUnitInSubtree(
                        tenantId,
                        grantedRootUnitId,
                        requestedUnitId
                );
    }

    private boolean matchesDirectReportsScope(
            UUID tenantId,
            UUID managerUserId,
            UUID managerAssignmentId,
            UUID targetUserId,
            Instant effectiveAt
    ) {
        if (managerAssignmentId == null
                || targetUserId == null) {
            return false;
        }

        AppUser targetUser =
                appUserRepository
                        .findByTenantIdAndId(
                                tenantId,
                                targetUserId
                        )
                        .orElse(null);

        if (targetUser == null
                || targetUser.getStatus()
                != UserStatus.ACTIVE) {
            return false;
        }

        return authorizationScopeQueryService
                .isDirectReport(
                        tenantId,
                        managerUserId,
                        managerAssignmentId,
                        targetUserId,
                        effectiveAt
                );
    }

    private boolean isActiveUnit(
            UUID tenantId,
            UUID organizationalUnitId
    ) {
        OrganizationalUnit unit =
                organizationalUnitRepository
                        .findByTenant_IdAndId(
                                tenantId,
                                organizationalUnitId
                        )
                        .orElse(null);

        return unit != null
                && unit.getStatus()
                == OrganizationalUnitStatus.ACTIVE;
    }

    private String normalizePermissionCode(
            String value
    ) {
        String normalized =
                value.trim()
                        .toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()
                || normalized.length() > 120) {
            return null;
        }

        return normalized;
    }
}