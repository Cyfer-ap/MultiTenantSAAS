package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuthorizationAccessContextType;
import com.chacha.multitenantsaas.dto.AuthorizationExplainAccessRequest;
import com.chacha.multitenantsaas.dto.AuthorizationExplainAccessResponse;
import com.chacha.multitenantsaas.dto.AuthorizationMatchedGrantResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.OrganizationAssignmentStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.UserOrganizationAssignment;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.OrganizationalUnitRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.UserOrganizationAssignmentRepository;
import com.chacha.multitenantsaas.security.AuthorizationEvaluationContext;
import com.chacha.multitenantsaas.security.AuthorizationPermissionDecision;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationAccessExplanationService {

    private final TenantLookupService tenantLookupService;

    private final AppUserRepository appUserRepository;

    private final ProjectRepository projectRepository;

    private final OrganizationalUnitRepository organizationalUnitRepository;

    private final UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;

    private final AuthorizationPermissionEvaluator authorizationPermissionEvaluator;

    public AuthorizationAccessExplanationService(
            TenantLookupService tenantLookupService,
            AppUserRepository appUserRepository,
            ProjectRepository projectRepository,
            OrganizationalUnitRepository organizationalUnitRepository,
            UserOrganizationAssignmentRepository userOrganizationAssignmentRepository,
            AuthorizationPermissionEvaluator authorizationPermissionEvaluator) {
        this.tenantLookupService = tenantLookupService;
        this.appUserRepository = appUserRepository;
        this.projectRepository = projectRepository;
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.userOrganizationAssignmentRepository = userOrganizationAssignmentRepository;
        this.authorizationPermissionEvaluator = authorizationPermissionEvaluator;
    }

    @Transactional(readOnly = true)
    public AuthorizationExplainAccessResponse explainAccess(
            UUID tenantId, AuthorizationExplainAccessRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Explain-access request is required.");
        }

        tenantLookupService.getActiveByIdOrThrow(tenantId);
        getRequiredActiveUser(tenantId, request.userId(), "Authorization subject user");

        Instant effectiveAt = normalizeDatabaseInstant(request.effectiveAt());
        AuthorizationEvaluationContext evaluationContext =
                buildAndValidateContext(tenantId, request, effectiveAt);

        AuthorizationPermissionDecision decision =
                authorizationPermissionEvaluator.evaluatePermission(
                        tenantId,
                        request.userId(),
                        request.permissionCode(),
                        evaluationContext,
                        effectiveAt);

        AuthorizationMatchedGrantResponse matchedGrant =
                decision.granted()
                        ? new AuthorizationMatchedGrantResponse(
                                decision.assignmentId(),
                                decision.roleId(),
                                decision.roleCode(),
                                decision.scopeType(),
                                decision.scopeTargetId(),
                                decision.validFrom(),
                                decision.validUntil())
                        : null;

        return new AuthorizationExplainAccessResponse(
                tenantId,
                request.userId(),
                decision.normalizedPermissionCode(),
                request.contextType(),
                request.targetId(),
                decision.granted(),
                decision.reason(),
                decision.evaluatedAt(),
                matchedGrant);
    }

    private AuthorizationEvaluationContext buildAndValidateContext(
            UUID tenantId, AuthorizationExplainAccessRequest request, Instant effectiveAt) {
        AuthorizationAccessContextType contextType = request.contextType();

        if (contextType == null) {
            throw new IllegalArgumentException("Access context type is required.");
        }

        return switch (contextType) {
            case TENANT -> {
                requireMissingTarget(contextType, request.targetId());
                yield AuthorizationEvaluationContext.tenant();
            }
            case USER -> {
                UUID targetUserId = requireTarget(contextType, request.targetId());
                getRequiredActiveUser(tenantId, targetUserId, "Authorization target user");
                yield AuthorizationEvaluationContext.user(targetUserId);
            }
            case PROJECT -> {
                UUID projectId = requireTarget(contextType, request.targetId());
                Project project =
                        projectRepository
                                .findByTenant_IdAndId(tenantId, projectId)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Authorization target project not found with id: "
                                                                + projectId));
                if (project.getStatus() == ProjectStatus.ARCHIVED) {
                    throw new ResourceNotFoundException(
                            "Authorization target project is not active with id: " + projectId);
                }
                yield AuthorizationEvaluationContext.project(projectId);
            }
            case ORGANIZATIONAL_UNIT -> {
                UUID unitId = requireTarget(contextType, request.targetId());
                requireActiveOrganizationalUnit(tenantId, unitId);
                yield AuthorizationEvaluationContext.organizationalUnit(unitId);
            }
            case ORGANIZATIONAL_SUBTREE -> {
                UUID unitId = requireTarget(contextType, request.targetId());
                requireActiveOrganizationalUnit(tenantId, unitId);
                yield AuthorizationEvaluationContext.organizationalSubtree(unitId);
            }
            case DIRECT_REPORTS_ANCHOR -> {
                UUID assignmentId = requireTarget(contextType, request.targetId());
                UserOrganizationAssignment assignment =
                        userOrganizationAssignmentRepository
                                .findByTenant_IdAndId(tenantId, assignmentId)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Direct-reports authorization anchor not found with id: "
                                                                + assignmentId));
                if (assignment.getStatus() != OrganizationAssignmentStatus.ACTIVE
                        || assignment.getUser().getStatus() != UserStatus.ACTIVE
                        || assignment.getOrganizationalUnit().getStatus()
                                != OrganizationalUnitStatus.ACTIVE
                        || assignment.getValidFrom().isAfter(effectiveAt)
                        || (assignment.getValidUntil() != null
                                && !assignment.getValidUntil().isAfter(effectiveAt))) {
                    throw new ResourceNotFoundException(
                            "Direct-reports authorization anchor is not active with id: "
                                    + assignmentId);
                }
                yield AuthorizationEvaluationContext.directReports(assignmentId);
            }
        };
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
            throw new ResourceNotFoundException(description + " is not active with id: " + userId);
        }

        return user;
    }

    private OrganizationalUnit requireActiveOrganizationalUnit(UUID tenantId, UUID unitId) {
        OrganizationalUnit unit =
                organizationalUnitRepository
                        .findByTenant_IdAndId(tenantId, unitId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Authorization target organizational unit not found with id: "
                                                        + unitId));

        if (unit.getStatus() != OrganizationalUnitStatus.ACTIVE) {
            throw new ResourceNotFoundException(
                    "Authorization target organizational unit is not active with id: " + unitId);
        }

        return unit;
    }

    private UUID requireTarget(AuthorizationAccessContextType contextType, UUID targetId) {
        if (targetId == null) {
            throw new IllegalArgumentException(contextType + " access context requires a target id.");
        }
        return targetId;
    }

    private void requireMissingTarget(AuthorizationAccessContextType contextType, UUID targetId) {
        if (targetId != null) {
            throw new IllegalArgumentException(
                    contextType + " access context must not contain a target id.");
        }
    }

    private Instant normalizeDatabaseInstant(Instant value) {
        return (value == null ? Instant.now() : value).truncatedTo(ChronoUnit.MICROS);
    }
}
