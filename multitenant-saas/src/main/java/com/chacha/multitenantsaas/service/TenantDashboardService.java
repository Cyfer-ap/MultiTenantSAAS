package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.TenantDashboardSummaryResponse;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectMemberRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.AuthenticatedUserContext;
import com.chacha.multitenantsaas.security.JwtContextService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class TenantDashboardService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final JwtContextService jwtContextService;

    public TenantDashboardService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectTaskRepository projectTaskRepository,
            JwtContextService jwtContextService) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectTaskRepository = projectTaskRepository;
        this.jwtContextService = jwtContextService;
    }

    public TenantDashboardSummaryResponse getTenantSummary(Jwt jwt) {
        AuthenticatedUserContext currentUser = jwtContextService.getCurrentUser(jwt);

        UUID tenantId = currentUser.tenantId();

        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(() -> new AuthenticationFailedException("Tenant not found"));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AuthenticationFailedException("Tenant is not active");
        }

        long totalProjects = projectRepository.countByTenant_Id(tenantId);

        long planningProjects =
                projectRepository.countByTenant_IdAndStatus(tenantId, ProjectStatus.PLANNING);

        long activeProjects =
                projectRepository.countByTenant_IdAndStatus(tenantId, ProjectStatus.ACTIVE);

        long onHoldProjects =
                projectRepository.countByTenant_IdAndStatus(tenantId, ProjectStatus.ON_HOLD);

        long completedProjects =
                projectRepository.countByTenant_IdAndStatus(tenantId, ProjectStatus.COMPLETED);

        long archivedProjects =
                projectRepository.countByTenant_IdAndStatus(tenantId, ProjectStatus.ARCHIVED);

        long totalProjectMemberships = projectMemberRepository.countByProject_Tenant_Id(tenantId);

        long totalTasks = projectTaskRepository.countByTenant_Id(tenantId);

        long todoTasks =
                projectTaskRepository.countByTenant_IdAndStatus(tenantId, ProjectTaskStatus.TODO);

        long inProgressTasks =
                projectTaskRepository.countByTenant_IdAndStatus(
                        tenantId, ProjectTaskStatus.IN_PROGRESS);

        long blockedTasks =
                projectTaskRepository.countByTenant_IdAndStatus(
                        tenantId, ProjectTaskStatus.BLOCKED);

        long completedTasks =
                projectTaskRepository.countByTenant_IdAndStatus(
                        tenantId, ProjectTaskStatus.COMPLETED);

        long cancelledTasks =
                projectTaskRepository.countByTenant_IdAndStatus(
                        tenantId, ProjectTaskStatus.CANCELLED);

        Instant currentTime = Instant.now();

        long overdueTasks =
                projectTaskRepository.countByTenant_IdAndDueAtBeforeAndStatusNotIn(
                        tenantId,
                        currentTime,
                        List.of(ProjectTaskStatus.COMPLETED, ProjectTaskStatus.CANCELLED));

        long completionEligibleTasks = totalTasks - cancelledTasks;

        double taskCompletionPercentage =
                completionEligibleTasks == 0
                        ? 0.0
                        : completedTasks * 100.0 / completionEligibleTasks;

        return new TenantDashboardSummaryResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getStatus(),
                appUserRepository.countByTenantId(tenantId),
                appUserRepository.countByTenantIdAndStatus(tenantId, UserStatus.ACTIVE),
                appUserRepository.countByTenantIdAndStatus(tenantId, UserStatus.INACTIVE),
                appUserRepository.countByTenantIdAndStatus(tenantId, UserStatus.SUSPENDED),
                totalProjects,
                planningProjects,
                activeProjects,
                onHoldProjects,
                completedProjects,
                archivedProjects,
                totalProjectMemberships,
                totalTasks,
                todoTasks,
                inProgressTasks,
                blockedTasks,
                completedTasks,
                cancelledTasks,
                overdueTasks,
                taskCompletionPercentage);
    }
}
