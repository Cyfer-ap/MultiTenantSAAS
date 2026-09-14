package com.chacha.multitenantsaas.projects.creation;

import com.chacha.multitenantsaas.dto.ProjectResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.AuditLogService;
import com.chacha.multitenantsaas.service.OutboundWebhookEventService;
import com.chacha.multitenantsaas.service.ProjectMemberService;
import com.chacha.multitenantsaas.service.SubscriptionQuotaGuardService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultProjectCreationAdapter implements ProjectCreationPort {

    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final ProjectMemberService projectMemberService;
    private final AuditLogService auditLogService;
    private final SubscriptionQuotaGuardService subscriptionQuotaGuardService;
    private final OutboundWebhookEventService outboundWebhookEventService;

    public DefaultProjectCreationAdapter(
            ProjectRepository projectRepository,
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            ProjectMemberService projectMemberService,
            AuditLogService auditLogService,
            SubscriptionQuotaGuardService subscriptionQuotaGuardService,
            OutboundWebhookEventService outboundWebhookEventService) {
        this.projectRepository = projectRepository;
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.projectMemberService = projectMemberService;
        this.auditLogService = auditLogService;
        this.subscriptionQuotaGuardService = subscriptionQuotaGuardService;
        this.outboundWebhookEventService = outboundWebhookEventService;
    }

    @Override
    @Transactional
    public ProjectCreationResult createProject(ProjectCreationCommand command) {
        Tenant tenant = requireActiveTenant(command.tenantId());
        subscriptionQuotaGuardService.requireProjectSlot(command.tenantId());
        AppUser actor = requireActiveActor(command.tenantId(), command.actorUserId());

        ProjectStatus initialStatus =
                command.initialStatus() == null ? ProjectStatus.PLANNING : command.initialStatus();
        if (initialStatus == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException("A project cannot be created in ARCHIVED status");
        }

        Project project =
                new Project(
                        tenant,
                        actor,
                        command.name().trim(),
                        normalizeDescription(command.description()));
        project.setStatus(initialStatus);
        Project savedProject = projectRepository.save(project);

        projectMemberService.addCreatorAsProjectLead(savedProject, actor);
        auditLogService.recordSuccess(
                tenant,
                actor,
                actor,
                AuditAction.PROJECT_CREATED,
                "Project created: " + savedProject.getId() + " - " + savedProject.getName());

        ProjectResponse response = mapResponse(savedProject);
        outboundWebhookEventService.publish(
                command.tenantId(), OutboundWebhookEventType.PROJECT_CREATED, response);

        return new ProjectCreationResult(
                response.id(),
                response.tenantId(),
                response.name(),
                response.description(),
                response.status(),
                response.createdByUserId(),
                response.createdByUserName(),
                response.createdByUserEmail(),
                response.createdAt(),
                response.updatedAt());
    }

    private Tenant requireActiveTenant(UUID tenantId) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Tenant not found with id: " + tenantId));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AuthenticationFailedException("Tenant is not active");
        }
        return tenant;
    }

    private AppUser requireActiveActor(UUID tenantId, UUID actorUserId) {
        AppUser actor =
                appUserRepository
                        .findByTenantIdAndId(tenantId, actorUserId)
                        .orElseThrow(
                                () ->
                                        new AuthenticationFailedException(
                                                "Authenticated user not found"));
        if (actor.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("Authenticated user account is not active");
        }
        return actor;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private ProjectResponse mapResponse(Project project) {
        AppUser createdBy = project.getCreatedByUser();
        return new ProjectResponse(
                project.getId(),
                project.getTenant().getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                createdBy.getId(),
                createdBy.getFullName(),
                createdBy.getEmail(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
