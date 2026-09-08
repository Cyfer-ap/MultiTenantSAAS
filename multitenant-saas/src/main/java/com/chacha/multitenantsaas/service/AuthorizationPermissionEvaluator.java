package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationDelegation;
import com.chacha.multitenantsaas.entity.AuthorizationDelegationStatus;
import com.chacha.multitenantsaas.entity.AuthorizationPermission;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionStatus;
import com.chacha.multitenantsaas.entity.AuthorizationRolePermission;
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
import com.chacha.multitenantsaas.repository.AuthorizationDelegationRepository;
import com.chacha.multitenantsaas.repository.AuthorizationRolePermissionRepository;
import com.chacha.multitenantsaas.repository.AuthorizationUserRoleAssignmentRepository;
import com.chacha.multitenantsaas.repository.OrganizationalUnitRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.AuthorizationAccessDecisionReason;
import com.chacha.multitenantsaas.security.AuthorizationEvaluationContext;
import com.chacha.multitenantsaas.security.AuthorizationPermissionDecision;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationPermissionEvaluator {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final AuthorizationUserRoleAssignmentRepository assignmentRepository;
    private final AuthorizationDelegationRepository delegationRepository;
    private final AuthorizationRolePermissionRepository rolePermissionRepository;
    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final ProjectRepository projectRepository;
    private final AuthorizationScopeQueryService authorizationScopeQueryService;

    public AuthorizationPermissionEvaluator(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            AuthorizationUserRoleAssignmentRepository assignmentRepository,
            AuthorizationDelegationRepository delegationRepository,
            AuthorizationRolePermissionRepository rolePermissionRepository,
            OrganizationalUnitRepository organizationalUnitRepository,
            ProjectRepository projectRepository,
            AuthorizationScopeQueryService authorizationScopeQueryService) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.assignmentRepository = assignmentRepository;
        this.delegationRepository = delegationRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.projectRepository = projectRepository;
        this.authorizationScopeQueryService = authorizationScopeQueryService;
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(
            UUID tenantId,
            UUID userId,
            String permissionCode,
            AuthorizationEvaluationContext context) {
        return evaluatePermission(tenantId, userId, permissionCode, context, Instant.now())
                .granted();
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(
            UUID tenantId,
            UUID userId,
            String permissionCode,
            AuthorizationEvaluationContext context,
            Instant effectiveAt) {
        return evaluatePermission(tenantId, userId, permissionCode, context, effectiveAt).granted();
    }

    @Transactional(readOnly = true)
    public AuthorizationPermissionDecision evaluatePermission(
            UUID tenantId,
            UUID userId,
            String permissionCode,
            AuthorizationEvaluationContext context) {
        return evaluatePermission(tenantId, userId, permissionCode, context, Instant.now());
    }

    @Transactional(readOnly = true)
    public AuthorizationPermissionDecision evaluatePermission(
            UUID tenantId,
            UUID userId,
            String permissionCode,
            AuthorizationEvaluationContext context,
            Instant effectiveAt) {
        Instant decisionAt =
                (effectiveAt == null ? Instant.now() : effectiveAt).truncatedTo(ChronoUnit.MICROS);

        if (tenantId == null || userId == null || permissionCode == null || effectiveAt == null) {
            return AuthorizationPermissionDecision.denied(
                    AuthorizationAccessDecisionReason.INVALID_INPUT, null, decisionAt);
        }

        String normalizedPermissionCode = normalizePermissionCode(permissionCode);
        if (normalizedPermissionCode == null) {
            return AuthorizationPermissionDecision.denied(
                    AuthorizationAccessDecisionReason.INVALID_PERMISSION_CODE, null, decisionAt);
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || tenant.getStatus() != TenantStatus.ACTIVE) {
            return AuthorizationPermissionDecision.denied(
                    AuthorizationAccessDecisionReason.TENANT_UNAVAILABLE,
                    normalizedPermissionCode,
                    decisionAt);
        }

        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return AuthorizationPermissionDecision.denied(
                    AuthorizationAccessDecisionReason.SUBJECT_UNAVAILABLE,
                    normalizedPermissionCode,
                    decisionAt);
        }

        AuthorizationEvaluationContext resolvedContext =
                context == null ? AuthorizationEvaluationContext.tenant() : context;

        List<AuthorizationUserRoleAssignment> effectiveGrants =
                assignmentRepository.findEffectiveAssignmentsGrantingPermission(
                        tenantId,
                        userId,
                        normalizedPermissionCode,
                        AuthorizationUserRoleAssignmentStatus.ACTIVE,
                        AuthorizationRoleStatus.ACTIVE,
                        AuthorizationPermissionStatus.ACTIVE,
                        AuthorizationPermissionSource.PLATFORM,
                        decisionAt);

        if (effectiveGrants.isEmpty()) {
            return AuthorizationPermissionDecision.denied(
                    AuthorizationAccessDecisionReason.NO_EFFECTIVE_GRANT,
                    normalizedPermissionCode,
                    decisionAt);
        }

        boolean rejectedDelegationSource = false;
        for (AuthorizationUserRoleAssignment grant : effectiveGrants) {
            if (!matchesScope(tenantId, userId, grant, resolvedContext, decisionAt)) {
                continue;
            }

            Optional<AuthorizationDelegation> delegation =
                    delegationRepository.findByTenant_IdAndDelegatedAssignment_Id(
                            tenantId, grant.getId());
            if (delegation.isEmpty()) {
                return AuthorizationPermissionDecision.granted(
                        normalizedPermissionCode, decisionAt, grant);
            }
            if (isDelegationSourceValid(
                    tenantId,
                    normalizedPermissionCode,
                    grant,
                    delegation.get(),
                    resolvedContext,
                    decisionAt)) {
                return AuthorizationPermissionDecision.granted(
                        normalizedPermissionCode, decisionAt, grant);
            }
            rejectedDelegationSource = true;
        }

        return AuthorizationPermissionDecision.denied(
                rejectedDelegationSource
                        ? AuthorizationAccessDecisionReason.DELEGATION_SOURCE_UNAVAILABLE
                        : AuthorizationAccessDecisionReason.SCOPE_NOT_SATISFIED,
                normalizedPermissionCode,
                decisionAt);
    }

    private boolean isDelegationSourceValid(
            UUID tenantId,
            String permissionCode,
            AuthorizationUserRoleAssignment delegatedGrant,
            AuthorizationDelegation delegation,
            AuthorizationEvaluationContext context,
            Instant effectiveAt) {
        if (delegation.getStatus() != AuthorizationDelegationStatus.ACTIVE
                || delegation.getRevokedAt() != null
                || !delegation.getDelegateUser().getId().equals(delegatedGrant.getUser().getId())
                || delegation.getDelegateUser().getStatus() != UserStatus.ACTIVE
                || delegation.getDelegatorUser().getStatus() != UserStatus.ACTIVE) {
            return false;
        }

        AuthorizationUserRoleAssignment parent = delegation.getParentAuthorityAssignment();
        if (!parent.getUser().getId().equals(delegation.getDelegatorUser().getId())
                || parent.getStatus() != AuthorizationUserRoleAssignmentStatus.ACTIVE
                || parent.getRole().getStatus() != AuthorizationRoleStatus.ACTIVE
                || parent.getUser().getStatus() != UserStatus.ACTIVE
                || parent.getValidFrom().isAfter(effectiveAt)
                || (parent.getValidUntil() != null && !parent.getValidUntil().isAfter(effectiveAt))) {
            return false;
        }

        if (delegationRepository.existsByTenant_IdAndDelegatedAssignment_Id(
                tenantId, parent.getId())) {
            return false;
        }
        if (!roleGrantsPermission(tenantId, parent.getRole().getId(), permissionCode)) {
            return false;
        }

        return matchesScope(
                tenantId, parent.getUser().getId(), parent, context, effectiveAt);
    }

    private boolean roleGrantsPermission(UUID tenantId, UUID roleId, String permissionCode) {
        return rolePermissionRepository.findRolePermissions(tenantId, roleId).stream()
                .map(AuthorizationRolePermission::getPermission)
                .filter(permission -> permission.getStatus() == AuthorizationPermissionStatus.ACTIVE)
                .filter(
                        permission ->
                                permission.getSource() == AuthorizationPermissionSource.PLATFORM
                                        || (permission.getTenant() != null
                                                && tenantId.equals(permission.getTenant().getId())))
                .map(AuthorizationPermission::getCode)
                .anyMatch(permissionCode::equals);
    }

    private boolean matchesScope(
            UUID tenantId,
            UUID userId,
            AuthorizationUserRoleAssignment grant,
            AuthorizationEvaluationContext context,
            Instant effectiveAt) {
        AuthorizationScopeType scopeType = grant.getScopeType();
        if (scopeType == null) {
            return false;
        }

        return switch (scopeType) {
            case TENANT -> true;
            case SELF -> context.targetUserId() != null && context.targetUserId().equals(userId);
            case PROJECT ->
                    matchesProjectScope(tenantId, grant.getScopeTargetId(), context.projectId());
            case ORGANIZATIONAL_UNIT ->
                    !context.requireSubtreeScope()
                            && matchesExactUnitScope(
                                    tenantId,
                                    grant.getScopeTargetId(),
                                    context.organizationalUnitId());
            case ORGANIZATIONAL_SUBTREE ->
                    matchesSubtreeScope(
                            tenantId, grant.getScopeTargetId(), context.organizationalUnitId());
            case DIRECT_REPORTS ->
                    matchesDirectReportsScope(
                            tenantId, userId, grant.getScopeTargetId(), context, effectiveAt);
        };
    }

    private boolean matchesProjectScope(
            UUID tenantId, UUID grantedProjectId, UUID requestedProjectId) {
        if (grantedProjectId == null || !grantedProjectId.equals(requestedProjectId)) {
            return false;
        }
        Project project =
                projectRepository.findByTenant_IdAndId(tenantId, requestedProjectId).orElse(null);
        return project != null && project.getStatus() != ProjectStatus.ARCHIVED;
    }

    private boolean matchesExactUnitScope(UUID tenantId, UUID grantedUnitId, UUID requestedUnitId) {
        if (grantedUnitId == null || !grantedUnitId.equals(requestedUnitId)) {
            return false;
        }
        return isActiveUnit(tenantId, requestedUnitId);
    }

    private boolean matchesSubtreeScope(
            UUID tenantId, UUID grantedRootUnitId, UUID requestedUnitId) {
        if (grantedRootUnitId == null || requestedUnitId == null) {
            return false;
        }
        if (!isActiveUnit(tenantId, grantedRootUnitId)) {
            return false;
        }
        if (!isActiveUnit(tenantId, requestedUnitId)) {
            return false;
        }
        return authorizationScopeQueryService.isUnitInSubtree(
                tenantId, grantedRootUnitId, requestedUnitId);
    }

    private boolean matchesDirectReportsScope(
            UUID tenantId,
            UUID managerUserId,
            UUID grantedManagerAssignmentId,
            AuthorizationEvaluationContext context,
            Instant effectiveAt) {
        if (grantedManagerAssignmentId == null) {
            return false;
        }

        UUID requestedManagerAssignmentId = context.directReportsManagerAssignmentId();
        if (requestedManagerAssignmentId != null) {
            if (!grantedManagerAssignmentId.equals(requestedManagerAssignmentId)) {
                return false;
            }
            return authorizationScopeQueryService.isDirectReportsAnchor(
                    tenantId, managerUserId, grantedManagerAssignmentId, effectiveAt);
        }

        UUID targetUserId = context.targetUserId();
        if (targetUserId == null) {
            return false;
        }

        AppUser targetUser =
                appUserRepository.findByTenantIdAndId(tenantId, targetUserId).orElse(null);
        if (targetUser == null || targetUser.getStatus() != UserStatus.ACTIVE) {
            return false;
        }
        return authorizationScopeQueryService.isDirectReport(
                tenantId, managerUserId, grantedManagerAssignmentId, targetUserId, effectiveAt);
    }

    private boolean isActiveUnit(UUID tenantId, UUID organizationalUnitId) {
        OrganizationalUnit unit =
                organizationalUnitRepository
                        .findByTenant_IdAndId(tenantId, organizationalUnitId)
                        .orElse(null);
        return unit != null && unit.getStatus() == OrganizationalUnitStatus.ACTIVE;
    }

    private String normalizePermissionCode(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 120) {
            return null;
        }
        return normalized;
    }
}
