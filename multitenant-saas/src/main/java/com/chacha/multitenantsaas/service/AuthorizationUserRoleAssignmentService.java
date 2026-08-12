package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationRole;
import com.chacha.multitenantsaas.entity.AuthorizationRoleStatus;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignment;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignmentStatus;
import com.chacha.multitenantsaas.entity.OrganizationAssignmentStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserOrganizationAssignment;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationRoleRepository;
import com.chacha.multitenantsaas.repository.AuthorizationUserRoleAssignmentRepository;
import com.chacha.multitenantsaas.repository.OrganizationalUnitRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.UserOrganizationAssignmentRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationUserRoleAssignmentService {

    private static final String TENANT_SCOPE_KEY = "TENANT";

    private static final String SELF_SCOPE_KEY = "SELF";

    private final TenantLookupService tenantLookupService;

    private final AppUserRepository appUserRepository;

    private final AuthorizationRoleRepository authorizationRoleRepository;

    private final AuthorizationUserRoleAssignmentRepository
            authorizationUserRoleAssignmentRepository;

    private final OrganizationalUnitRepository organizationalUnitRepository;

    private final UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;

    private final ProjectRepository projectRepository;

    public AuthorizationUserRoleAssignmentService(
            TenantLookupService tenantLookupService,
            AppUserRepository appUserRepository,
            AuthorizationRoleRepository authorizationRoleRepository,
            AuthorizationUserRoleAssignmentRepository authorizationUserRoleAssignmentRepository,
            OrganizationalUnitRepository organizationalUnitRepository,
            UserOrganizationAssignmentRepository userOrganizationAssignmentRepository,
            ProjectRepository projectRepository) {
        this.tenantLookupService = tenantLookupService;

        this.appUserRepository = appUserRepository;

        this.authorizationRoleRepository = authorizationRoleRepository;

        this.authorizationUserRoleAssignmentRepository = authorizationUserRoleAssignmentRepository;

        this.organizationalUnitRepository = organizationalUnitRepository;

        this.userOrganizationAssignmentRepository = userOrganizationAssignmentRepository;

        this.projectRepository = projectRepository;
    }

    @Transactional
    public AuthorizationUserRoleAssignmentResponse createAssignment(
            UUID tenantId,
            UUID createdByUserId,
            AuthorizationUserRoleAssignmentCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("User-role assignment request " + "is required.");
        }

        if (request.userId() == null) {
            throw new IllegalArgumentException("Assigned user id is required.");
        }

        if (request.roleId() == null) {
            throw new IllegalArgumentException("Authorization role id is required.");
        }

        if (request.scopeType() == null) {
            throw new IllegalArgumentException("Authorization scope type is required.");
        }

        Tenant tenant = tenantLookupService.getActiveByIdOrThrow(tenantId);

        AppUser assignedUser = getRequiredActiveUser(tenantId, request.userId(), "Assigned user");

        AppUser createdByUser = getRequiredActiveUser(tenantId, createdByUserId, "Creating user");

        AuthorizationRole role = getRequiredActiveRole(tenantId, request.roleId());

        Instant validFrom =
                normalizeDatabaseInstant(
                        request.validFrom() == null ? Instant.now() : request.validFrom());

        Instant validUntil = normalizeDatabaseInstant(request.validUntil());

        validateValidityRange(validFrom, validUntil);

        String scopeKey =
                validateScopeAndBuildKey(
                        tenantId,
                        assignedUser,
                        request.scopeType(),
                        request.scopeTargetId(),
                        validFrom,
                        validUntil);

        validateNoOverlappingAssignment(
                tenantId,
                assignedUser.getId(),
                role.getId(),
                request.scopeType(),
                scopeKey,
                validFrom,
                validUntil);

        AuthorizationUserRoleAssignment assignment =
                new AuthorizationUserRoleAssignment(
                        tenant,
                        assignedUser,
                        role,
                        request.scopeType(),
                        request.scopeTargetId(),
                        scopeKey,
                        validFrom,
                        validUntil,
                        createdByUser);

        AuthorizationUserRoleAssignment savedAssignment =
                authorizationUserRoleAssignmentRepository.saveAndFlush(assignment);

        return mapToResponse(savedAssignment);
    }

    @Transactional(readOnly = true)
    public AuthorizationUserRoleAssignmentResponse getAssignment(UUID tenantId, UUID assignmentId) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        return mapToResponse(getAssignmentOrThrow(tenantId, assignmentId));
    }

    @Transactional(readOnly = true)
    public List<AuthorizationUserRoleAssignmentResponse> getUserAssignments(
            UUID tenantId, UUID userId) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        getUserOrThrow(tenantId, userId, "User");

        return authorizationUserRoleAssignmentRepository
                .findUserAssignments(tenantId, userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorizationUserRoleAssignmentResponse> getEffectiveUserAssignments(
            UUID tenantId, UUID userId, Instant effectiveAt) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        getUserOrThrow(tenantId, userId, "User");

        Instant resolvedEffectiveAt =
                normalizeDatabaseInstant(effectiveAt == null ? Instant.now() : effectiveAt);

        return authorizationUserRoleAssignmentRepository
                .findEffectiveAssignmentsForUser(
                        tenantId,
                        userId,
                        AuthorizationUserRoleAssignmentStatus.ACTIVE,
                        AuthorizationRoleStatus.ACTIVE,
                        resolvedEffectiveAt)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorizationUserRoleAssignmentResponse> getRoleAssignments(
            UUID tenantId, UUID roleId) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        getRoleOrThrow(tenantId, roleId);

        return authorizationUserRoleAssignmentRepository
                .findRoleAssignments(tenantId, roleId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public AuthorizationUserRoleAssignmentResponse deactivateAssignment(
            UUID tenantId, UUID assignmentId) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        AuthorizationUserRoleAssignment assignment = getAssignmentOrThrow(tenantId, assignmentId);

        if (assignment.getStatus() == AuthorizationUserRoleAssignmentStatus.INACTIVE) {
            return mapToResponse(assignment);
        }

        Instant now = normalizeDatabaseInstant(Instant.now());

        assignment.setStatus(AuthorizationUserRoleAssignmentStatus.INACTIVE);

        if (!now.isBefore(assignment.getValidFrom())
                && (assignment.getValidUntil() == null
                        || assignment.getValidUntil().isAfter(now))) {
            assignment.setValidUntil(now);
        }

        AuthorizationUserRoleAssignment savedAssignment =
                authorizationUserRoleAssignmentRepository.saveAndFlush(assignment);

        return mapToResponse(savedAssignment);
    }

    private String validateScopeAndBuildKey(
            UUID tenantId,
            AppUser assignedUser,
            AuthorizationScopeType scopeType,
            UUID scopeTargetId,
            Instant validFrom,
            Instant validUntil) {
        return switch (scopeType) {
            case TENANT -> {
                requireMissingScopeTarget(scopeType, scopeTargetId);

                yield TENANT_SCOPE_KEY;
            }

            case SELF -> {
                requireMissingScopeTarget(scopeType, scopeTargetId);

                yield SELF_SCOPE_KEY;
            }

            case ORGANIZATIONAL_UNIT, ORGANIZATIONAL_SUBTREE -> {
                requireScopeTarget(scopeType, scopeTargetId);

                OrganizationalUnit unit =
                        organizationalUnitRepository
                                .findByTenant_IdAndId(tenantId, scopeTargetId)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Organizational "
                                                                + "scope unit "
                                                                + "not found "
                                                                + "with id: "
                                                                + scopeTargetId));

                if (unit.getStatus() != OrganizationalUnitStatus.ACTIVE) {
                    throw new IllegalArgumentException(
                            "Organizational scope unit " + "must be active.");
                }

                yield scopeTargetId.toString();
            }

            case PROJECT -> {
                requireScopeTarget(scopeType, scopeTargetId);

                Project project =
                        projectRepository
                                .findByTenant_IdAndId(tenantId, scopeTargetId)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Project scope target "
                                                                + "not found "
                                                                + "with id: "
                                                                + scopeTargetId));

                if (project.getStatus() == ProjectStatus.ARCHIVED) {
                    throw new IllegalArgumentException(
                            "Archived project cannot be "
                                    + "used as an authorization "
                                    + "scope target.");
                }

                yield scopeTargetId.toString();
            }

            case DIRECT_REPORTS -> {
                requireScopeTarget(scopeType, scopeTargetId);

                UserOrganizationAssignment managerAssignment =
                        userOrganizationAssignmentRepository
                                .findByTenant_IdAndId(tenantId, scopeTargetId)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Manager "
                                                                + "organizational "
                                                                + "assignment "
                                                                + "not found "
                                                                + "with id: "
                                                                + scopeTargetId));

                validateDirectReportsAnchor(assignedUser, managerAssignment, validFrom, validUntil);

                yield scopeTargetId.toString();
            }
        };
    }

    private void validateDirectReportsAnchor(
            AppUser assignedUser,
            UserOrganizationAssignment managerAssignment,
            Instant validFrom,
            Instant validUntil) {
        if (managerAssignment.getStatus() != OrganizationAssignmentStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Direct-reports scope requires " + "an active organizational " + "assignment.");
        }

        if (!managerAssignment.getUser().getId().equals(assignedUser.getId())) {
            throw new IllegalArgumentException(
                    "Direct-reports scope target must "
                            + "belong to the user receiving "
                            + "the authorization role.");
        }

        if (managerAssignment.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Direct-reports scope user " + "must be active.");
        }

        if (managerAssignment.getOrganizationalUnit().getStatus()
                != OrganizationalUnitStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Direct-reports organizational unit " + "must be active.");
        }

        if (managerAssignment.getValidFrom().isAfter(validFrom)) {
            throw new IllegalArgumentException(
                    "Direct-reports scope assignment "
                            + "must begin on or before "
                            + "the role assignment.");
        }

        Instant anchorValidUntil = managerAssignment.getValidUntil();

        if (anchorValidUntil != null
                && (validUntil == null || anchorValidUntil.isBefore(validUntil))) {
            throw new IllegalArgumentException(
                    "Direct-reports scope assignment "
                            + "must remain valid for the "
                            + "complete role-assignment "
                            + "period.");
        }
    }

    private void validateNoOverlappingAssignment(
            UUID tenantId,
            UUID userId,
            UUID roleId,
            AuthorizationScopeType scopeType,
            String scopeKey,
            Instant validFrom,
            Instant validUntil) {
        long overlappingAssignmentCount =
                authorizationUserRoleAssignmentRepository.countOverlappingActiveAssignments(
                        tenantId,
                        userId,
                        roleId,
                        scopeType,
                        scopeKey,
                        AuthorizationUserRoleAssignmentStatus.ACTIVE,
                        validFrom,
                        validUntil);

        if (overlappingAssignmentCount > 0) {
            throw new DuplicateResourceException(
                    "User already has an overlapping "
                            + "active assignment for this "
                            + "role and authorization scope.");
        }
    }

    private void requireScopeTarget(AuthorizationScopeType scopeType, UUID scopeTargetId) {
        if (scopeTargetId == null) {
            throw new IllegalArgumentException(
                    scopeType + " scope requires " + "a scope target id.");
        }
    }

    private void requireMissingScopeTarget(AuthorizationScopeType scopeType, UUID scopeTargetId) {
        if (scopeTargetId != null) {
            throw new IllegalArgumentException(
                    scopeType + " scope must not contain " + "a scope target id.");
        }
    }

    private AuthorizationRole getRequiredActiveRole(UUID tenantId, UUID roleId) {
        AuthorizationRole role = getRoleOrThrow(tenantId, roleId);

        if (role.getStatus() != AuthorizationRoleStatus.ACTIVE) {
            throw new IllegalArgumentException("Authorization role must be active.");
        }

        return role;
    }

    private AuthorizationRole getRoleOrThrow(UUID tenantId, UUID roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Authorization role id is required.");
        }

        return authorizationRoleRepository
                .findByTenant_IdAndId(tenantId, roleId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Authorization role "
                                                + "not found "
                                                + "with id: "
                                                + roleId));
    }

    private AppUser getRequiredActiveUser(UUID tenantId, UUID userId, String description) {
        AppUser user = getUserOrThrow(tenantId, userId, description);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(description + " must be active.");
        }

        return user;
    }

    private AppUser getUserOrThrow(UUID tenantId, UUID userId, String description) {
        if (userId == null) {
            throw new IllegalArgumentException(description + " id is required.");
        }

        return appUserRepository
                .findByTenantIdAndId(tenantId, userId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        description + " not found " + "with id: " + userId));
    }

    private AuthorizationUserRoleAssignment getAssignmentOrThrow(UUID tenantId, UUID assignmentId) {
        if (assignmentId == null) {
            throw new IllegalArgumentException("User-role assignment id is required.");
        }

        return authorizationUserRoleAssignmentRepository
                .findByTenant_IdAndId(tenantId, assignmentId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "User-role assignment "
                                                + "not found "
                                                + "with id: "
                                                + assignmentId));
    }

    private void validateValidityRange(Instant validFrom, Instant validUntil) {
        if (validUntil != null && !validUntil.isAfter(validFrom)) {
            throw new IllegalArgumentException(
                    "Role-assignment valid-until time " + "must be after valid-from time.");
        }
    }

    private Instant normalizeDatabaseInstant(Instant value) {
        if (value == null) {
            return null;
        }

        return value.truncatedTo(ChronoUnit.MICROS);
    }

    private AuthorizationUserRoleAssignmentResponse mapToResponse(
            AuthorizationUserRoleAssignment assignment) {
        return new AuthorizationUserRoleAssignmentResponse(
                assignment.getId(),
                assignment.getTenant().getId(),
                assignment.getUser().getId(),
                assignment.getUser().getFullName(),
                assignment.getUser().getEmail(),
                assignment.getRole().getId(),
                assignment.getRole().getCode(),
                assignment.getRole().getName(),
                assignment.getRole().getSource(),
                assignment.getScopeType(),
                assignment.getScopeTargetId(),
                assignment.getStatus(),
                assignment.getValidFrom(),
                assignment.getValidUntil(),
                assignment.getCreatedByUser().getId(),
                assignment.getCreatedByUser().getEmail(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }
}
