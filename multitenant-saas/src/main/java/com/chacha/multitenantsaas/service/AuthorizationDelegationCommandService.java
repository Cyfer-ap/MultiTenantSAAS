package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuthorizationDelegationCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationDelegationReferenceDataResponse;
import com.chacha.multitenantsaas.dto.AuthorizationDelegationResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.security.AuthorizationEvaluationContext;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationDelegationCommandService {

    private final AuthorizationDelegationService authorizationDelegationService;
    private final AuthorizationDelegationReferenceDataService
            authorizationDelegationReferenceDataService;
    private final AuthorizationPermissionEvaluator authorizationPermissionEvaluator;
    private final CurrentActorService currentActorService;
    private final AuditLogService auditLogService;

    public AuthorizationDelegationCommandService(
            AuthorizationDelegationService authorizationDelegationService,
            AuthorizationDelegationReferenceDataService authorizationDelegationReferenceDataService,
            AuthorizationPermissionEvaluator authorizationPermissionEvaluator,
            CurrentActorService currentActorService,
            AuditLogService auditLogService) {
        this.authorizationDelegationService = authorizationDelegationService;
        this.authorizationDelegationReferenceDataService =
                authorizationDelegationReferenceDataService;
        this.authorizationPermissionEvaluator = authorizationPermissionEvaluator;
        this.currentActorService = currentActorService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthorizationDelegationResponse createDelegation(
            UUID tenantId, AuthorizationDelegationCreateRequest request, Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        AuthorizationDelegationResponse response =
                authorizationDelegationService.createDelegation(tenantId, actor.getId(), request);

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.AUTH_DELEGATION_CREATED,
                "Authorization delegated: delegationId="
                        + response.id()
                        + "; delegateUserId="
                        + response.delegateUserId()
                        + "; roleCode="
                        + response.roleCode()
                        + "; scopeType="
                        + response.scopeType()
                        + "; scopeTargetId="
                        + formatNullableUuid(response.scopeTargetId())
                        + "; parentAssignmentId="
                        + response.parentAssignmentId()
                        + "; validUntil="
                        + response.validUntil());

        return response;
    }

    @Transactional(readOnly = true)
    public List<AuthorizationDelegationResponse> getVisibleDelegations(UUID tenantId, Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return authorizationDelegationService.getVisibleDelegations(
                tenantId, actor.getId(), canManageAuthorization(tenantId, actor.getId()));
    }

    @Transactional(readOnly = true)
    public AuthorizationDelegationReferenceDataResponse getReferenceData(UUID tenantId, Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return authorizationDelegationReferenceDataService.getReferenceData(
                tenantId, actor.getId());
    }

    @Transactional
    public AuthorizationDelegationResponse revokeDelegation(
            UUID tenantId, UUID delegationId, Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        AuthorizationDelegationResponse response =
                authorizationDelegationService.revokeDelegation(
                        tenantId,
                        delegationId,
                        actor.getId(),
                        canManageAuthorization(tenantId, actor.getId()));

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.AUTH_DELEGATION_REVOKED,
                "Authorization delegation revoked: delegationId="
                        + response.id()
                        + "; delegateUserId="
                        + response.delegateUserId()
                        + "; delegatedAssignmentId="
                        + response.delegatedAssignmentId());

        return response;
    }

    private boolean canManageAuthorization(UUID tenantId, UUID userId) {
        return authorizationPermissionEvaluator.hasPermission(
                tenantId,
                userId,
                PlatformPermissionCodes.AUTHORIZATION_MANAGE,
                AuthorizationEvaluationContext.tenant());
    }

    private String formatNullableUuid(UUID value) {
        return value == null ? "NONE" : value.toString();
    }
}
