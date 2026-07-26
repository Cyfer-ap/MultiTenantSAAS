package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.*;
import com.chacha.multitenantsaas.entity.*;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final CurrentActorService currentActorService;
    private final ProjectMemberService projectMemberService;
    private final AuditLogService auditLogService;

    public ProjectService(
            ProjectRepository projectRepository,
            TenantRepository tenantRepository,
            CurrentActorService currentActorService,
            ProjectMemberService projectMemberService,
            AuditLogService auditLogService
    ) {
        this.projectRepository = projectRepository;
        this.tenantRepository = tenantRepository;
        this.currentActorService = currentActorService;
        this.projectMemberService = projectMemberService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ProjectResponse createProject(
            UUID tenantId,
            ProjectCreateRequest request,
            Jwt jwt
    ) {
        Tenant tenant = getRequiredActiveTenant(tenantId);

        AppUser actor =
                currentActorService.getRequiredActiveActor(
                        tenantId,
                        jwt
                );

        Project project = new Project(
                tenant,
                actor,
                request.name().trim(),
                normalizeDescription(request.description())
        );

        Project savedProject =
                projectRepository.save(project);

        projectMemberService.addCreatorAsProjectLead(
                savedProject,
                actor
        );

        auditLogService.recordSuccess(
                tenant,
                actor,
                actor,
                AuditAction.PROJECT_CREATED,
                "Project created: "
                        + savedProject.getId()
                        + " - "
                        + savedProject.getName()
        );

        return mapToResponse(savedProject);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getProjects(
            UUID tenantId,
            ProjectStatus status,
            String search,
            Pageable pageable
    ) {
        getRequiredActiveTenant(tenantId);

        Page<Project> projects =
                projectRepository.findTenantProjects(
                        tenantId,
                        status,
                        normalizeSearch(search),
                        pageable
                );

        return new PageResponse<>(
                projects.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList(),
                projects.getNumber(),
                projects.getSize(),
                projects.getTotalElements(),
                projects.getTotalPages(),
                projects.isFirst(),
                projects.isLast()
        );
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(
            UUID tenantId,
            UUID projectId
    ) {
        return mapToResponse(
                getProjectOrThrow(tenantId, projectId)
        );
    }

    @Transactional
    public ProjectResponse updateProject(
            UUID tenantId,
            UUID projectId,
            ProjectUpdateRequest request,
            Jwt jwt
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        ensureProjectIsNotArchived(project);

        AppUser actor =
                currentActorService.getRequiredActiveActor(
                        tenantId,
                        jwt
                );

        project.setName(request.name().trim());
        project.setDescription(
                normalizeDescription(request.description())
        );

        Project updatedProject =
                projectRepository.save(project);

        auditLogService.recordSuccess(
                project.getTenant(),
                actor,
                actor,
                AuditAction.PROJECT_UPDATED,
                "Project updated: "
                        + updatedProject.getId()
                        + " - "
                        + updatedProject.getName()
        );

        return mapToResponse(updatedProject);
    }

    @Transactional
    public ProjectResponse updateProjectStatus(
            UUID tenantId,
            UUID projectId,
            ProjectStatusUpdateRequest request,
            Jwt jwt
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        ensureProjectIsNotArchived(project);

        if (request.status() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "Use the archive endpoint to archive a project"
            );
        }

        AppUser actor =
                currentActorService.getRequiredActiveActor(
                        tenantId,
                        jwt
                );

        ProjectStatus previousStatus = project.getStatus();

        project.setStatus(request.status());

        Project updatedProject =
                projectRepository.save(project);

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
                        + project.getId()
        );

        return mapToResponse(updatedProject);
    }

    @Transactional
    public ProjectResponse archiveProject(
            UUID tenantId,
            UUID projectId,
            Jwt jwt
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "Project is already archived"
            );
        }

        AppUser actor =
                currentActorService.getRequiredActiveActor(
                        tenantId,
                        jwt
                );

        project.setStatus(ProjectStatus.ARCHIVED);

        Project archivedProject =
                projectRepository.save(project);

        auditLogService.recordSuccess(
                project.getTenant(),
                actor,
                actor,
                AuditAction.PROJECT_ARCHIVED,
                "Project archived: "
                        + project.getId()
                        + " - "
                        + project.getName()
        );

        return mapToResponse(archivedProject);
    }

    private Tenant getRequiredActiveTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant not found with id: " + tenantId
                ));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AuthenticationFailedException(
                    "Tenant is not active"
            );
        }

        return tenant;
    }

    private Project getProjectOrThrow(
            UUID tenantId,
            UUID projectId
    ) {
        return projectRepository
                .findByTenant_IdAndId(
                        tenantId,
                        projectId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: "
                                + projectId
                                + " for tenant: "
                                + tenantId
                ));
    }

    private void ensureProjectIsNotArchived(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "Archived project cannot be modified"
            );
        }
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }

        String normalized = search.trim();

        return normalized.isBlank()
                ? null
                : normalized;
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
                project.getUpdatedAt()
        );
    }
}