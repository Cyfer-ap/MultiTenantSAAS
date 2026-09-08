package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuthorizationDelegationCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationDelegationResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationDelegation;
import com.chacha.multitenantsaas.entity.AuthorizationDelegationStatus;
import com.chacha.multitenantsaas.entity.AuthorizationPermission;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionStatus;
import com.chacha.multitenantsaas.entity.AuthorizationRole;
import com.chacha.multitenantsaas.entity.AuthorizationRolePermission;
import com.chacha.multitenantsaas.entity.AuthorizationRoleStatus;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignment;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignmentStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationDelegationRepository;
import com.chacha.multitenantsaas.repository.AuthorizationRolePermissionRepository;
import com.chacha.multitenantsaas.repository.AuthorizationRoleRepository;
import com.chacha.multitenantsaas.repository.AuthorizationUserRoleAssignmentRepository;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationDelegationService {

    private final TenantLookupService tenantLookupService;
    private final AppUserRepository appUserRepository;
    private final AuthorizationRoleRepository authorizationRoleRepository;
    private final AuthorizationRolePermissionRepository authorizationRolePermissionRepository;
    private final AuthorizationUserRoleAssignmentRepository assignmentRepository;
    private final AuthorizationDelegationRepository delegationRepository;
    private final AuthorizationUserRoleAssignmentService assignmentService;
    private final AuthorizationScopeQueryService authorizationScopeQueryService;

    public AuthorizationDelegationService(
            TenantLookupService tenantLookupService,
            AppUserRepository appUserRepository,
            AuthorizationRoleRepository authorizationRoleRepository,
            AuthorizationRolePermissionRepository authorizationRolePermissionRepository,
            AuthorizationUserRoleAssignmentRepository assignmentRepository,
            AuthorizationDelegationRepository delegationRepository,
            AuthorizationUserRoleAssignmentService assignmentService,
            AuthorizationScopeQueryService authorizationScopeQueryService) {
        this.tenantLookupService = tenantLookupService;
        this.appUserRepository = appUserRepository;
        this.authorizationRoleRepository = authorizationRoleRepository;
        this.authorizationRolePermissionRepository = authorizationRolePermissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.delegationRepository = delegationRepository;
        this.assignmentService = assignmentService;
        this.authorizationScopeQueryService = authorizationScopeQueryService;
    }

    @Transactional
    public AuthorizationDelegationResponse createDelegation(
            UUID tenantId, UUID delegatorUserId, AuthorizationDelegationCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Authorization delegation request is required.");
        }
        if (request.delegateUserId() == null
                || request.parentAssignmentId() == null
                || request.roleId() == null
                || request.scopeType() == null
                || request.validUntil() == null) {
            throw new IllegalArgumentException("Delegation request is missing required fields.");
        }

        Tenant tenant = tenantLookupService.getActiveByIdOrThrow(tenantId);
        AppUser delegator = getRequiredActiveUser(tenantId, delegatorUserId, "Delegator");
        AppUser delegate = getRequiredActiveUser(tenantId, request.delegateUserId(), "Delegate");

        if (delegator.getId().equals(delegate.getId())) {
            throw new IllegalArgumentException("Authorization cannot be delegated to the same user.");
        }

        AuthorizationUserRoleAssignment parentAssignment =
                getAssignmentOrThrow(tenantId, request.parentAssignmentId());
        validateDirectParentAuthority(parentAssignment, delegator);

        AuthorizationRole delegatedRole = getRequiredActiveRole(tenantId, request.roleId());
        validatePermissionContainment(tenantId, parentAssignment.getRole(), delegatedRole);
        validateScopeContainment(
                tenantId,
                parentAssignment,
                request.scopeType(),
                request.scopeTargetId());

        Instant now = normalizeDatabaseInstant(Instant.now());
        Instant validFrom =
                normalizeDatabaseInstant(request.validFrom() == null ? now : request.validFrom());
        Instant validUntil = normalizeDatabaseInstant(request.validUntil());
        validateTimeContainment(parentAssignment, now, validFrom, validUntil);

        AuthorizationUserRoleAssignmentResponse assignmentResponse =
                assignmentService.createAssignment(
                        tenantId,
                        delegator.getId(),
                        new AuthorizationUserRoleAssignmentCreateRequest(
                                delegate.getId(),
                                delegatedRole.getId(),
                                request.scopeType(),
                                request.scopeTargetId(),
                                validFrom,
                                validUntil));

        AuthorizationUserRoleAssignment delegatedAssignment =
                getAssignmentOrThrow(tenantId, assignmentResponse.id());

        AuthorizationDelegation delegation =
                new AuthorizationDelegation(
                        tenant,
                        delegator,
                        delegate,
                        parentAssignment,
                        delegatedAssignment);

        return mapToResponse(delegationRepository.saveAndFlush(delegation));
    }

    @Transactional(readOnly = true)
    public List<AuthorizationDelegationResponse> getVisibleDelegations(
            UUID tenantId, UUID viewerUserId, boolean canManageAll) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);
        getRequiredActiveUser(tenantId, viewerUserId, "Viewing user");

        List<AuthorizationDelegation> delegations =
                canManageAll
                        ? delegationRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId)
                        : delegationRepository
                                .findByTenant_IdAndDelegatorUser_IdOrderByCreatedAtDesc(
                                        tenantId, viewerUserId);

        return delegations.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public AuthorizationDelegationResponse revokeDelegation(
            UUID tenantId, UUID delegationId, UUID actorUserId, boolean canManageAll) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);
        AppUser actor = getRequiredActiveUser(tenantId, actorUserId, "Revoking user");
        AuthorizationDelegation delegation = getDelegationOrThrow(tenantId, delegationId);

        if (!delegation.getDelegatorUser().getId().equals(actor.getId()) && !canManageAll) {
            throw new AccessDeniedException(
                    "Only the delegator or an authorization administrator can revoke this delegation.");
        }

        if (delegation.getStatus() == AuthorizationDelegationStatus.REVOKED) {
            return mapToResponse(delegation);
        }

        Instant now = normalizeDatabaseInstant(Instant.now());
        assignmentService.deactivateAssignment(
                tenantId, delegation.getDelegatedAssignment().getId());
        delegation.revoke(actor, now);

        return mapToResponse(delegationRepository.saveAndFlush(delegation));
    }

    private void validateDirectParentAuthority(
            AuthorizationUserRoleAssignment parentAssignment, AppUser delegator) {
        if (!parentAssignment.getUser().getId().equals(delegator.getId())) {
            throw new IllegalArgumentException(
                    "Parent authority assignment must belong to the delegator.");
        }
        if (parentAssignment.getStatus() != AuthorizationUserRoleAssignmentStatus.ACTIVE
                || parentAssignment.getRole().getStatus() != AuthorizationRoleStatus.ACTIVE) {
            throw new IllegalArgumentException("Parent authority assignment must be active.");
        }
        if (delegationRepository.existsByTenant_IdAndDelegatedAssignment_Id(
                parentAssignment.getTenant().getId(), parentAssignment.getId())) {
            throw new IllegalArgumentException("Delegated authority cannot be re-delegated.");
        }
        if (parentAssignment.getScopeType() == AuthorizationScopeType.SELF
                || parentAssignment.getScopeType() == AuthorizationScopeType.DIRECT_REPORTS) {
            throw new IllegalArgumentException(
                    "SELF and DIRECT_REPORTS authority cannot be delegated in this version.");
        }
    }

    private void validatePermissionContainment(
            UUID tenantId, AuthorizationRole parentRole, AuthorizationRole delegatedRole) {
        Set<String> parentPermissions = getActivePermissionCodes(tenantId, parentRole.getId());
        Set<String> delegatedPermissions = getActivePermissionCodes(tenantId, delegatedRole.getId());

        if (delegatedPermissions.isEmpty()) {
            throw new IllegalArgumentException("Delegated role must contain at least one active permission.");
        }
        if (delegatedPermissions.contains(PlatformPermissionCodes.AUTHORIZATION_MANAGE)
                || delegatedPermissions.contains(PlatformPermissionCodes.AUTHORIZATION_DELEGATE)) {
            throw new IllegalArgumentException(
                    "Authorization management and delegation permissions cannot be delegated.");
        }
        if (!parentPermissions.containsAll(delegatedPermissions)) {
            throw new IllegalArgumentException(
                    "Delegated role contains permissions outside the parent authority assignment.");
        }
    }

    private Set<String> getActivePermissionCodes(UUID tenantId, UUID roleId) {
        return authorizationRolePermissionRepository.findRolePermissions(tenantId, roleId).stream()
                .map(AuthorizationRolePermission::getPermission)
                .filter(permission -> isActiveAccessiblePermission(tenantId, permission))
                .map(AuthorizationPermission::getCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isActiveAccessiblePermission(UUID tenantId, AuthorizationPermission permission) {
        if (permission.getStatus() != AuthorizationPermissionStatus.ACTIVE) {
            return false;
        }
        if (permission.getSource() == AuthorizationPermissionSource.PLATFORM) {
            return true;
        }
        return permission.getTenant() != null && tenantId.equals(permission.getTenant().getId());
    }

    private void validateScopeContainment(
            UUID tenantId,
            AuthorizationUserRoleAssignment parentAssignment,
            AuthorizationScopeType delegatedScopeType,
            UUID delegatedScopeTargetId) {
        if (delegatedScopeType == AuthorizationScopeType.DIRECT_REPORTS) {
            throw new IllegalArgumentException(
                    "DIRECT_REPORTS authority cannot be delegated in this version.");
        }

        boolean contained =
                switch (parentAssignment.getScopeType()) {
                    case TENANT -> true;
                    case PROJECT ->
                            delegatedScopeType == AuthorizationScopeType.PROJECT
                                    && parentAssignment
                                            .getScopeTargetId()
                                            .equals(delegatedScopeTargetId);
                    case ORGANIZATIONAL_UNIT ->
                            delegatedScopeType == AuthorizationScopeType.ORGANIZATIONAL_UNIT
                                    && parentAssignment
                                            .getScopeTargetId()
                                            .equals(delegatedScopeTargetId);
                    case ORGANIZATIONAL_SUBTREE ->
                            (delegatedScopeType == AuthorizationScopeType.ORGANIZATIONAL_UNIT
                                            || delegatedScopeType
                                                    == AuthorizationScopeType.ORGANIZATIONAL_SUBTREE)
                                    && delegatedScopeTargetId != null
                                    && authorizationScopeQueryService.isUnitInSubtree(
                                            tenantId,
                                            parentAssignment.getScopeTargetId(),
                                            delegatedScopeTargetId);
                    case SELF, DIRECT_REPORTS -> false;
                };

        if (!contained) {
            throw new IllegalArgumentException(
                    "Delegated scope must be equal to or narrower than the parent authority scope.");
        }
    }

    private void validateTimeContainment(
            AuthorizationUserRoleAssignment parentAssignment,
            Instant now,
            Instant validFrom,
            Instant validUntil) {
        if (parentAssignment.getValidFrom().isAfter(now)) {
            throw new IllegalArgumentException(
                    "Parent authority assignment must already be effective.");
        }
        if (validFrom.isBefore(now)) {
            throw new IllegalArgumentException("Delegation valid-from time cannot be in the past.");
        }
        if (!validUntil.isAfter(validFrom) || !validUntil.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Delegation valid-until time must be after valid-from and current time.");
        }
        if (parentAssignment.getValidFrom().isAfter(validFrom)) {
            throw new IllegalArgumentException(
                    "Delegation cannot start before its parent authority assignment.");
        }
        Instant parentValidUntil = parentAssignment.getValidUntil();
        if (parentValidUntil != null && parentValidUntil.isBefore(validUntil)) {
            throw new IllegalArgumentException(
                    "Delegation cannot outlive its parent authority assignment.");
        }
        if (parentValidUntil != null && !parentValidUntil.isAfter(now)) {
            throw new IllegalArgumentException("Parent authority assignment has expired.");
        }
    }

    private AppUser getRequiredActiveUser(UUID tenantId, UUID userId, String description) {
        if (userId == null) {
            throw new IllegalArgumentException(description + " id is required.");
        }
        AppUser user =
                appUserRepository
                        .findByTenantIdAndId(tenantId, userId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                description + " not found with id: " + userId));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(description + " must be active.");
        }
        return user;
    }

    private AuthorizationRole getRequiredActiveRole(UUID tenantId, UUID roleId) {
        AuthorizationRole role =
                authorizationRoleRepository
                        .findByTenant_IdAndId(tenantId, roleId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Authorization role not found with id: " + roleId));
        if (role.getStatus() != AuthorizationRoleStatus.ACTIVE) {
            throw new IllegalArgumentException("Authorization role must be active.");
        }
        return role;
    }

    private AuthorizationUserRoleAssignment getAssignmentOrThrow(UUID tenantId, UUID assignmentId) {
        return assignmentRepository
                .findByTenant_IdAndId(tenantId, assignmentId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "User-role assignment not found with id: " + assignmentId));
    }

    private AuthorizationDelegation getDelegationOrThrow(UUID tenantId, UUID delegationId) {
        if (delegationId == null) {
            throw new IllegalArgumentException("Authorization delegation id is required.");
        }
        return delegationRepository
                .findByTenant_IdAndId(tenantId, delegationId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Authorization delegation not found with id: " + delegationId));
    }

    private Instant normalizeDatabaseInstant(Instant value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.MICROS);
    }

    private AuthorizationDelegationResponse mapToResponse(AuthorizationDelegation delegation) {
        AuthorizationUserRoleAssignment assignment = delegation.getDelegatedAssignment();
        AppUser revokedBy = delegation.getRevokedByUser();

        return new AuthorizationDelegationResponse(
                delegation.getId(),
                delegation.getTenant().getId(),
                delegation.getDelegatorUser().getId(),
                delegation.getDelegatorUser().getEmail(),
                delegation.getDelegateUser().getId(),
                delegation.getDelegateUser().getEmail(),
                delegation.getParentAuthorityAssignment().getId(),
                assignment.getId(),
                assignment.getRole().getId(),
                assignment.getRole().getCode(),
                assignment.getRole().getName(),
                assignment.getScopeType(),
                assignment.getScopeTargetId(),
                delegation.getStatus(),
                assignment.getValidFrom(),
                assignment.getValidUntil(),
                delegation.getCreatedAt(),
                delegation.getRevokedAt(),
                revokedBy == null ? null : revokedBy.getId(),
                revokedBy == null ? null : revokedBy.getEmail());
    }
}
