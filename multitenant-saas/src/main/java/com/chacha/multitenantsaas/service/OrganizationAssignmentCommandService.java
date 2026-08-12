package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.OrganizationAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.OrganizationAssignmentResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationAssignmentCommandService {

    private final OrganizationAssignmentService organizationAssignmentService;

    private final CurrentActorService currentActorService;

    private final AppUserRepository appUserRepository;

    private final AuditLogService auditLogService;

    public OrganizationAssignmentCommandService(
            OrganizationAssignmentService organizationAssignmentService,
            CurrentActorService currentActorService,
            AppUserRepository appUserRepository,
            AuditLogService auditLogService) {
        this.organizationAssignmentService = organizationAssignmentService;

        this.currentActorService = currentActorService;

        this.appUserRepository = appUserRepository;

        this.auditLogService = auditLogService;
    }

    @Transactional
    public OrganizationAssignmentResponse createAssignment(
            UUID tenantId, OrganizationAssignmentCreateRequest request, Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);

        OrganizationAssignmentResponse assignment =
                organizationAssignmentService.createAssignment(tenantId, actor.getId(), request);

        AppUser assignedUser = getTargetUser(tenantId, assignment.userId());

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                assignedUser,
                AuditAction.ORG_ASSIGNMENT_CREATED,
                "Organizational assignment created: "
                        + assignment.id()
                        + "; userId="
                        + assignment.userId()
                        + "; organizationalUnitId="
                        + assignment.organizationalUnitId()
                        + "; primaryAssignment="
                        + assignment.primaryAssignment()
                        + "; reportsToAssignmentId="
                        + formatNullableUuid(assignment.reportsToAssignmentId()));

        return assignment;
    }

    @Transactional
    public OrganizationAssignmentResponse deactivateAssignment(
            UUID tenantId, UUID assignmentId, Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);

        OrganizationAssignmentResponse existingAssignment =
                organizationAssignmentService.getAssignment(tenantId, assignmentId);

        AppUser assignedUser = getTargetUser(tenantId, existingAssignment.userId());

        OrganizationAssignmentResponse deactivatedAssignment =
                organizationAssignmentService.deactivateAssignment(tenantId, assignmentId);

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                assignedUser,
                AuditAction.ORG_ASSIGNMENT_DEACTIVATED,
                "Organizational assignment deactivated: "
                        + deactivatedAssignment.id()
                        + "; userId="
                        + deactivatedAssignment.userId()
                        + "; organizationalUnitId="
                        + deactivatedAssignment.organizationalUnitId()
                        + "; validUntil="
                        + deactivatedAssignment.validUntil());

        return deactivatedAssignment;
    }

    private AppUser getTargetUser(UUID tenantId, UUID userId) {
        return appUserRepository
                .findByTenantIdAndId(tenantId, userId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Assigned user not found " + "with id: " + userId));
    }

    private String formatNullableUuid(UUID value) {
        return value == null ? "NONE" : value.toString();
    }
}
