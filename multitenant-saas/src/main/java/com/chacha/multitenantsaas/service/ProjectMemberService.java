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
    private final AuditLogService auditLogService;

    public ProjectMemberService(
            ProjectMemberRepository projectMemberRepository,
            ProjectRepository projectRepository,
            AppUserRepository appUserRepository,
            CurrentActorService currentActorService,
            AuditLogService auditLogService
    ) {
        this.projectMemberRepository = projectMemberRepository;
        this.projectRepository = projectRepository;
        this.appUserRepository = appUserRepository;
        this.currentActorService = currentActorService;
        this.auditLogService = auditLogService;
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

        ProjectMember savedMembership =
                projectMemberRepository.save(membership);

        auditLogService.recordSuccess(
                project.getTenant(),
                assignedBy,
                user,
                AuditAction.PROJECT_MEMBER_ADDED,
                "User added to project "
                        + projectId
                        + " as "
                        + request.role()
                        + ": "
                        + user.getEmail()
        );

        return mapToResponse(savedMembership);
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
            ProjectMemberRoleUpdateRequest request,
            Jwt jwt
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        ensureProjectCanBeModified(project);

        AppUser actor =
                currentActorService.getRequiredActiveActor(
                        tenantId,
                        jwt
                );

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

        ProjectMemberRole previousRole =
                membership.getRole();

        membership.setRole(request.role());

        ProjectMember updatedMembership =
                projectMemberRepository.save(membership);

        auditLogService.recordSuccess(
                project.getTenant(),
                actor,
                membership.getUser(),
                AuditAction.PROJECT_MEMBER_ROLE_UPDATED,
                "Project member role changed from "
                        + previousRole
                        + " to "
                        + request.role()
                        + " for project "
                        + projectId
                        + ": "
                        + membership.getUser().getEmail()
        );

        return mapToResponse(updatedMembership);
    }

    @Transactional
    public ProjectMemberResponse removeMember(
            UUID tenantId,
            UUID projectId,
            UUID userId,
            Jwt jwt
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        ensureProjectCanBeModified(project);

        AppUser actor =
                currentActorService.getRequiredActiveActor(
                        tenantId,
                        jwt
                );

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

        AppUser removedUser = membership.getUser();

        ProjectMemberResponse response =
                mapToResponse(membership);

        projectMemberRepository.delete(membership);

        auditLogService.recordSuccess(
                project.getTenant(),
                actor,
                removedUser,
                AuditAction.PROJECT_MEMBER_REMOVED,
                "User removed from project "
                        + projectId
                        + ": "
                        + removedUser.getEmail()
        );

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