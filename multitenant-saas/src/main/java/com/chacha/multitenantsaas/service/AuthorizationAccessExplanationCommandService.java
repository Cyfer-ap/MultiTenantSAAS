package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuthorizationExplainAccessRequest;
import com.chacha.multitenantsaas.dto.AuthorizationExplainAccessResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationAccessExplanationCommandService {

    private final AuthorizationAccessExplanationService authorizationAccessExplanationService;

    private final CurrentActorService currentActorService;

    private final AuditLogService auditLogService;

    public AuthorizationAccessExplanationCommandService(
            AuthorizationAccessExplanationService authorizationAccessExplanationService,
            CurrentActorService currentActorService,
            AuditLogService auditLogService) {
        this.authorizationAccessExplanationService = authorizationAccessExplanationService;
        this.currentActorService = currentActorService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthorizationExplainAccessResponse explainAccess(
            UUID tenantId, AuthorizationExplainAccessRequest request, Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);

        AuthorizationExplainAccessResponse response =
                authorizationAccessExplanationService.explainAccess(tenantId, request);

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.AUTH_ACCESS_EXPLAINED,
                "Authorization access explained: subjectUserId="
                        + response.userId()
                        + "; permission="
                        + response.permissionCode()
                        + "; contextType="
                        + response.contextType()
                        + "; targetId="
                        + formatNullableUuid(response.targetId())
                        + "; granted="
                        + response.granted()
                        + "; reason="
                        + response.reason()
                        + "; matchedAssignmentId="
                        + formatNullableUuid(
                                response.matchedGrant() == null
                                        ? null
                                        : response.matchedGrant().assignmentId()));

        return response;
    }

    private String formatNullableUuid(UUID value) {
        return value == null ? "NONE" : value.toString();
    }
}
