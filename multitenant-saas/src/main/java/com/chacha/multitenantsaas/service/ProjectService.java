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

    public ProjectService(
            ProjectRepository projectRepository,
            TenantRepository tenantRepository,
            CurrentActorService currentActorService
    ) {
        this.projectRepository = projectRepository;
        this.tenantRepository = tenantRepository;
        this.currentActorService = currentActorService;
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
            ProjectUpdateRequest request
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        ensureProjectIsNotArchived(project);

        project.setName(request.name().trim());
        project.setDescription(
                normalizeDescription(request.description())
        );

        return mapToResponse(
                projectRepository.save(project)
        );
    }

    @Transactional
    public ProjectResponse updateProjectStatus(
            UUID tenantId,
            UUID projectId,
            ProjectStatusUpdateRequest request
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        ensureProjectIsNotArchived(project);

        if (request.status() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "Use the archive endpoint to archive a project"
            );
        }

        project.setStatus(request.status());

        return mapToResponse(
                projectRepository.save(project)
        );
    }

    @Transactional
    public ProjectResponse archiveProject(
            UUID tenantId,
            UUID projectId
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "Project is already archived"
            );
        }

        project.setStatus(ProjectStatus.ARCHIVED);

        return mapToResponse(
                projectRepository.save(project)
        );
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