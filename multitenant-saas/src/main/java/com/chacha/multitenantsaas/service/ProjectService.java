package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.*;
import com.chacha.multitenantsaas.entity.*;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.projects.creation.ProjectCreationCommand;
import com.chacha.multitenantsaas.projects.creation.ProjectCreationPort;
import com.chacha.multitenantsaas.projects.creation.ProjectCreationResult;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final CurrentActorService currentActorService;
    private final AuditLogService auditLogService;
    private final OutboundWebhookEventService outboundWebhookEventService;
    private final ProjectCreationPort projectCreationPort;

    public ProjectService(
            ProjectRepository projectRepository,
            TenantRepository tenantRepository,
            CurrentActorService currentActorService,
            AuditLogService auditLogService,
            OutboundWebhookEventService outboundWebhookEventService,
            ProjectCreationPort projectCreationPort) {
        this.projectRepository = projectRepository;
        this.tenantRepository = tenantRepository;
        this.currentActorService = currentActorService;
        this.auditLogService = auditLogService;
        this.outboundWebhookEventService = outboundWebhookEventService;
        this.projectCreationPort = projectCreationPort;
    }

    @Transactional
    public ProjectResponse createProject(UUID tenantId, ProjectCreateRequest request, Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        ProjectCreationResult created =
                projectCreationPort.createProject(
                        new ProjectCreationCommand(
                                tenantId,
                                actor.getId(),
                                request.name(),
                                request.description(),
                                ProjectStatus.PLANNING));
        return mapToResponse(created);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getProjects(
            UUID tenantId, ProjectStatus status, String search, Pageable pageable) {
        getRequiredActiveTenant(tenantId);

        Page<Project> projects =
                projectRepository.findTenantProjects(
                        tenantId, status, normalizeSearch(search), pageable);

        return new PageResponse<>(
                projects.getContent().stream().map(this::mapToResponse).toList(),
                projects.getNumber(),
                projects.getSize(),
                projects.getTotalElements(),
                projects.getTotalPages(),
                projects.isFirst(),
                projects.isLast());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID tenantId, UUID projectId) {
        return mapToResponse(getProjectOrThrow(tenantId, projectId));
    }

    @Transactional
    public ProjectResponse updateProject(
            UUID tenantId, UUID projectId, ProjectUpdateRequest request, Jwt jwt) {
        Project project = getProjectOrThrow(tenantId, projectId);

        ensureProjectIsNotArchived(project);

        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);

        project.setName(request.name().trim());
        project.setDescription(normalizeDescription(request.description()));

        Project updatedProject = projectRepository.save(project);

        auditLogService.recordSuccess(
                project.getTenant(),
                actor,
                actor,
                AuditAction.PROJECT_UPDATED,
                "Project updated: " + updatedProject.getId() + " - " + updatedProject.getName());

        ProjectResponse response = mapToResponse(updatedProject);
        publish(tenantId, OutboundWebhookEventType.PROJECT_UPDATED, response);
        return response;
    }

    @Transactional
    public ProjectResponse updateProjectStatus(
            UUID tenantId, UUID projectId, ProjectStatusUpdateRequest request, Jwt jwt) {
        Project project = getProjectOrThrow(tenantId, projectId);

        ensureProjectIsNotArchived(project);

        if (request.status() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException("Use the archive endpoint to archive a project");
        }

        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);

        ProjectStatus previousStatus = project.getStatus();

        project.setStatus(request.status());

        Project updatedProject = projectRepository.save(project);

        auditLogService.recordSuccess(
                project.getTenant(),
                actor,
                actor,
                AuditAction.PROJECT_STATUS_UPDATED,
                "Project status changed from "
                        + previousStatus
                        + " to "
                        + request.status()
                        + ": "
                        + project.getId());

        ProjectResponse response = mapToResponse(updatedProject);
        publish(tenantId, OutboundWebhookEventType.PROJECT_UPDATED, response);
        return response;
    }

    @Transactional
    public ProjectResponse archiveProject(UUID tenantId, UUID projectId, Jwt jwt) {
        Project project = getProjectOrThrow(tenantId, projectId);

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException("Project is already archived");
        }

        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);

        project.setStatus(ProjectStatus.ARCHIVED);

        Project archivedProject = projectRepository.save(project);

        auditLogService.recordSuccess(
                project.getTenant(),
                actor,
                actor,
                AuditAction.PROJECT_ARCHIVED,
                "Project archived: " + project.getId() + " - " + project.getName());

        ProjectResponse response = mapToResponse(archivedProject);
        publish(tenantId, OutboundWebhookEventType.PROJECT_ARCHIVED, response);
        return response;
    }

    private Tenant getRequiredActiveTenant(UUID tenantId) {
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

    private Project getProjectOrThrow(UUID tenantId, UUID projectId) {
        return projectRepository
                .findByTenant_IdAndId(tenantId, projectId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Project not found with id: "
                                                + projectId
                                                + " for tenant: "
                                                + tenantId));
    }

    private void ensureProjectIsNotArchived(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException("Archived project cannot be modified");
        }
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();

        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }

        String normalized = search.trim();

        return normalized.isBlank() ? null : normalized;
    }

    private void publish(
            UUID tenantId, OutboundWebhookEventType eventType, ProjectResponse response) {
        if (outboundWebhookEventService != null) {
            outboundWebhookEventService.publish(tenantId, eventType, response);
        }
    }

    private ProjectResponse mapToResponse(Project project) {
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

    private ProjectResponse mapToResponse(ProjectCreationResult project) {
        return new ProjectResponse(
                project.projectId(),
                project.tenantId(),
                project.name(),
                project.description(),
                project.status(),
                project.createdByUserId(),
                project.createdByUserName(),
                project.createdByUserEmail(),
                project.createdAt(),
                project.updatedAt());
    }
}
