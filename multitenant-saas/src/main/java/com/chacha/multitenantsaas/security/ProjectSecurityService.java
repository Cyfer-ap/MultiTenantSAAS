package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.entity.*;
import com.chacha.multitenantsaas.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("projectSecurity")
public class ProjectSecurityService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTaskRepository projectTaskRepository;

    public ProjectSecurityService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectTaskRepository projectTaskRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectTaskRepository = projectTaskRepository;
    }

    public boolean canReadTasks(
            UUID tenantId,
            UUID projectId
    ) {
        AppUser user = getActiveUserForTenant(tenantId);

        if (user == null || !projectExists(tenantId, projectId)) {
            return false;
        }

        if (isTenantAdminOrManager(user)) {
            return true;
        }

        return projectMemberRepository
                .existsByProject_Tenant_IdAndProject_IdAndUser_Id(
                        tenantId,
                        projectId,
                        user.getId()
                );
    }

    public boolean canManageTasks(
            UUID tenantId,
            UUID projectId
    ) {
        AppUser user = getActiveUserForTenant(tenantId);

        if (user == null || !projectExists(tenantId, projectId)) {
            return false;
        }

        if (isTenantAdminOrManager(user)) {
            return true;
        }

        return projectMemberRepository
                .findByProject_Tenant_IdAndProject_IdAndUser_Id(
                        tenantId,
                        projectId,
                        user.getId()
                )
                .map(ProjectMember::getRole)
                .filter(role ->
                        role == ProjectMemberRole.PROJECT_LEAD
                )
                .isPresent();
    }

    public boolean canUpdateTaskStatus(
            UUID tenantId,
            UUID projectId,
            UUID taskId
    ) {
        AppUser user = getActiveUserForTenant(tenantId);

        if (user == null || !projectExists(tenantId, projectId)) {
            return false;
        }

        if (isTenantAdminOrManager(user)) {
            return true;
        }

        boolean projectLead = projectMemberRepository
                .findByProject_Tenant_IdAndProject_IdAndUser_Id(
                        tenantId,
                        projectId,
                        user.getId()
                )
                .map(ProjectMember::getRole)
                .filter(role ->
                        role == ProjectMemberRole.PROJECT_LEAD
                )
                .isPresent();

        if (projectLead) {
            return true;
        }

        return projectTaskRepository
                .findByProject_Tenant_IdAndProject_IdAndId(
                        tenantId,
                        projectId,
                        taskId
                )
                .map(ProjectTask::getAssigneeUser)
                .map(AppUser::getId)
                .filter(user.getId()::equals)
                .isPresent();
    }

    private boolean projectExists(
            UUID tenantId,
            UUID projectId
    ) {
        return projectRepository
                .findByTenant_IdAndId(
                        tenantId,
                        projectId
                )
                .isPresent();
    }

    private boolean isTenantAdminOrManager(
            AppUser user
    ) {
        return user.getRole() == UserRole.TENANT_ADMIN
                || user.getRole() == UserRole.TENANT_MANAGER;
    }

    private AppUser getActiveUserForTenant(UUID tenantId) {
        Jwt jwt = getJwt();

        if (jwt == null) {
            return null;
        }

        UUID tokenTenantId =
                parseUuid(jwt.getClaimAsString("tenantId"));

        UUID tokenUserId =
                parseUuid(jwt.getSubject());

        if (tokenTenantId == null
                || tokenUserId == null
                || !tenantId.equals(tokenTenantId)) {
            return null;
        }

        Tenant tenant = tenantRepository
                .findById(tenantId)
                .orElse(null);

        if (tenant == null
                || tenant.getStatus() != TenantStatus.ACTIVE) {
            return null;
        }

        AppUser user = appUserRepository
                .findByTenantIdAndId(
                        tenantId,
                        tokenUserId
                )
                .orElse(null);

        if (user == null
                || user.getStatus() != UserStatus.ACTIVE) {
            return null;
        }

        return user;
    }

    private UUID parseUuid(String value) {
        try {
            return value == null
                    ? null
                    : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Jwt getJwt() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof Jwt jwt)) {
            return null;
        }

        return jwt;
    }
}