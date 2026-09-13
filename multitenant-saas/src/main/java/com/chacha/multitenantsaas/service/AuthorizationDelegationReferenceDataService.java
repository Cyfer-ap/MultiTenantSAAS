package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuthorizationAssignmentReferenceDataResponse;
import com.chacha.multitenantsaas.dto.AuthorizationDelegationReferenceDataResponse;
import com.chacha.multitenantsaas.dto.AuthorizationPermissionResponse;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.repository.AuthorizationDelegationRepository;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationDelegationReferenceDataService {

    private final AuthorizationAssignmentReferenceDataService assignmentReferenceDataService;
    private final AuthorizationRoleService authorizationRoleService;
    private final AuthorizationUserRoleAssignmentService assignmentService;
    private final AuthorizationDelegationRepository delegationRepository;

    public AuthorizationDelegationReferenceDataService(
            AuthorizationAssignmentReferenceDataService assignmentReferenceDataService,
            AuthorizationRoleService authorizationRoleService,
            AuthorizationUserRoleAssignmentService assignmentService,
            AuthorizationDelegationRepository delegationRepository) {
        this.assignmentReferenceDataService = assignmentReferenceDataService;
        this.authorizationRoleService = authorizationRoleService;
        this.assignmentService = assignmentService;
        this.delegationRepository = delegationRepository;
    }

    @Transactional(readOnly = true)
    public AuthorizationDelegationReferenceDataResponse getReferenceData(
            UUID tenantId, UUID delegatorUserId) {
        AuthorizationAssignmentReferenceDataResponse assignmentReferenceData =
                assignmentReferenceDataService.getReferenceData(tenantId);

        List<AuthorizationRoleResponse> roles =
                authorizationRoleService.getActiveRoles(tenantId).stream()
                        .filter(this::isDelegableRole)
                        .toList();

        List<AuthorizationUserRoleAssignmentResponse> parentAssignments =
                assignmentService.getEffectiveUserAssignments(tenantId, delegatorUserId, Instant.now())
                        .stream()
                        .filter(this::isDelegableParentScope)
                        .filter(
                                assignment ->
                                        !delegationRepository
                                                .existsByTenant_IdAndDelegatedAssignment_Id(
                                                        tenantId, assignment.id()))
                        .toList();

        return new AuthorizationDelegationReferenceDataResponse(
                assignmentReferenceData.users().stream()
                        .filter(user -> !user.id().equals(delegatorUserId))
                        .toList(),
                roles,
                parentAssignments,
                assignmentReferenceData.organizationalUnits(),
                assignmentReferenceData.projects());
    }

    private boolean isDelegableRole(AuthorizationRoleResponse role) {
        if (role.permissions().isEmpty()) {
            return false;
        }

        return role.permissions().stream()
                .map(AuthorizationPermissionResponse::code)
                .noneMatch(
                        code ->
                                PlatformPermissionCodes.AUTHORIZATION_MANAGE.equals(code)
                                        || PlatformPermissionCodes.AUTHORIZATION_DELEGATE.equals(
                                                code));
    }

    private boolean isDelegableParentScope(AuthorizationUserRoleAssignmentResponse assignment) {
        return assignment.scopeType() != AuthorizationScopeType.SELF
                && assignment.scopeType() != AuthorizationScopeType.DIRECT_REPORTS;
    }
}
