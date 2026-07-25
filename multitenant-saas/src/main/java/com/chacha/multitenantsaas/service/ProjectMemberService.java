package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.ProjectMemberAddRequest;
import com.chacha.multitenantsaas.dto.ProjectMemberResponse;
import com.chacha.multitenantsaas.dto.ProjectMemberRoleUpdateRequest;
import com.chacha.multitenantsaas.entity.*;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectMemberRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentActorService currentActorService;

    public ProjectMemberService(
            ProjectMemberRepository projectMemberRepository,
            ProjectRepository projectRepository,
            AppUserRepository appUserRepository,
            CurrentActorService currentActorService
    ) {
        this.projectMemberRepository = projectMemberRepository;
        this.projectRepository = projectRepository;
        this.appUserRepository = appUserRepository;
        this.currentActorService = currentActorService;
    }

    @Transactional
    public ProjectMemberResponse addMember(
            UUID tenantId,
            UUID projectId,
            ProjectMemberAddRequest request,
            Jwt jwt
    ) {
        Project project = getProjectOrThrow(
                tenantId,
                projectId
        );

        ensureProjectCanBeModified(project);

        AppUser assignedBy =
                currentActorService.getRequiredActiveActor(
                        tenantId,
                        jwt
                );

        AppUser user = getActiveTenantUserOrThrow(
                tenantId,
                request.userId()
        );

        if (projectMemberRepository
                .existsByProject_IdAndUser_Id(
                        projectId,
                        user.getId()
                )) {
            throw new DuplicateResourceException(
                    "User is already assigned to this project"
            );
        }

        ProjectMember membership = new ProjectMember(
                project,
                user,
                assignedBy,
                request.role()
        );

        return mapToResponse(
                projectMemberRepository.save(membership)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectMemberResponse> getMembers(
            UUID tenantId,
            UUID projectId,
            ProjectMemberRole role,
            String search,
            Pageable pageable
    ) {
        getProjectOrThrow(tenantId, projectId);

        Page<ProjectMember> memberships =
                projectMemberRepository.findProjectMembers(
                        tenantId,
                        projectId,
                        role,
                        normalizeSearch(search),
                        pageable
                );

        return new PageResponse<>(
                memberships.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList(),
                memberships.getNumber(),
                memberships.getSize(),
                memberships.getTotalElements(),
                memberships.getTotalPages(),
                memberships.isFirst(),
                memberships.isLast()
        );
    }

    @Transactional(readOnly = true)
    public ProjectMemberResponse getMember(
            UUID tenantId,
            UUID projectId,
            UUID userId
    ) {
        return mapToResponse(
                getMembershipOrThrow(
                        tenantId,
                        projectId,
                        userId
                )
        );
    }

    @Transactional
    public ProjectMemberResponse updateMemberRole(
            UUID tenantId,
            UUID projectId,
            UUID userId,
            ProjectMemberRoleUpdateRequest request
    ) {
        Project project = getProjectOrThrow(
                tenantId,
                projectId
        );

        ensureProjectCanBeModified(project);

        ProjectMember membership =
                getMembershipOrThrow(
                        tenantId,
                        projectId,
                        userId
                );

        if (membership.getRole() == request.role()) {
            return mapToResponse(membership);
        }

        if (membership.getRole()
                == ProjectMemberRole.PROJECT_LEAD
                && request.role()
                != ProjectMemberRole.PROJECT_LEAD) {

            ensureAnotherProjectLeadExists(projectId);
        }

        membership.setRole(request.role());

        return mapToResponse(
                projectMemberRepository.save(membership)
        );
    }

    @Transactional
    public ProjectMemberResponse removeMember(
            UUID tenantId,
            UUID projectId,
            UUID userId
    ) {
        Project project = getProjectOrThrow(
                tenantId,
                projectId
        );

        ensureProjectCanBeModified(project);

        ProjectMember membership =
                getMembershipOrThrow(
                        tenantId,
                        projectId,
                        userId
                );

        if (membership.getRole()
                == ProjectMemberRole.PROJECT_LEAD) {
            ensureAnotherProjectLeadExists(projectId);
        }

        ProjectMemberResponse response =
                mapToResponse(membership);

        projectMemberRepository.delete(membership);

        return response;
    }

    @Transactional
    public void addCreatorAsProjectLead(
            Project project,
            AppUser creator
    ) {
        if (projectMemberRepository
                .existsByProject_IdAndUser_Id(
                        project.getId(),
                        creator.getId()
                )) {
            return;
        }

        ProjectMember membership = new ProjectMember(
                project,
                creator,
                creator,
                ProjectMemberRole.PROJECT_LEAD
        );

        projectMemberRepository.save(membership);
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

    private AppUser getActiveTenantUserOrThrow(
            UUID tenantId,
            UUID userId
    ) {
        AppUser user = appUserRepository
                .findByTenantIdAndId(
                        tenantId,
                        userId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: "
                                + userId
                                + " for tenant: "
                                + tenantId
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Only active tenant users can be assigned to projects"
            );
        }

        return user;
    }

    private ProjectMember getMembershipOrThrow(
            UUID tenantId,
            UUID projectId,
            UUID userId
    ) {
        return projectMemberRepository
                .findByProject_Tenant_IdAndProject_IdAndUser_Id(
                        tenantId,
                        projectId,
                        userId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project membership not found for user: "
                                + userId
                ));
    }

    private void ensureProjectCanBeModified(
            Project project
    ) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "Archived project memberships cannot be modified"
            );
        }
    }

    private void ensureAnotherProjectLeadExists(
            UUID projectId
    ) {
        long projectLeadCount =
                projectMemberRepository
                        .countByProject_IdAndRole(
                                projectId,
                                ProjectMemberRole.PROJECT_LEAD
                        );

        if (projectLeadCount <= 1) {
            throw new IllegalArgumentException(
                    "Project must have at least one project lead"
            );
        }
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

    private ProjectMemberResponse mapToResponse(
            ProjectMember membership
    ) {
        AppUser member = membership.getUser();
        AppUser assignedBy = membership.getAssignedByUser();

        return new ProjectMemberResponse(
                membership.getId(),
                membership.getProject().getId(),
                member.getId(),
                member.getFullName(),
                member.getEmail(),
                member.getRole(),
                member.getStatus(),
                membership.getRole(),
                assignedBy.getId(),
                assignedBy.getFullName(),
                assignedBy.getEmail(),
                membership.getAssignedAt(),
                membership.getUpdatedAt()
        );
    }
}