package com.chacha.multitenantsaas.tasks.calendar;

import com.chacha.multitenantsaas.calendar.spi.CalendarDeadlineContext;
import com.chacha.multitenantsaas.calendar.spi.CalendarDeadlineSnapshot;
import com.chacha.multitenantsaas.calendar.spi.CalendarDeadlineSource;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.projects.query.ProjectMembershipQueryService;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class TaskCalendarDeadlineSource implements CalendarDeadlineSource {

    private static final int MAX_FETCH = 500;

    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectMembershipQueryService projectMembershipQueryService;
    private final AuthorizationSecurityService authorizationSecurity;

    public TaskCalendarDeadlineSource(
            ProjectTaskRepository projectTaskRepository,
            ProjectMembershipQueryService projectMembershipQueryService,
            AuthorizationSecurityService authorizationSecurity) {
        this.projectTaskRepository = projectTaskRepository;
        this.projectMembershipQueryService = projectMembershipQueryService;
        this.authorizationSecurity = authorizationSecurity;
    }

    @Override
    public List<CalendarDeadlineSnapshot> findDeadlines(
            CalendarDeadlineContext context, Instant from, Instant to, int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, MAX_FETCH));
        boolean tenantWide =
                authorizationSecurity.hasTenantPermission(
                        context.tenantId(), PlatformPermissionCodes.PROJECT_TASK_READ);

        Set<UUID> readableProjectIds = projectScopedIds(context);
        readableProjectIds.addAll(
                projectMembershipQueryService.findProjectIdsForUser(
                        context.tenantId(), context.userId()));

        if (!tenantWide && readableProjectIds.isEmpty()) {
            return List.of();
        }

        var pageable =
                PageRequest.of(
                        0,
                        limit,
                        Sort.by(
                                Sort.Order.asc("dueAt"),
                                Sort.Order.desc("updatedAt"),
                                Sort.Order.asc("id")));

        Page<com.chacha.multitenantsaas.entity.ProjectTask> candidates =
                tenantWide
                        ? projectTaskRepository
                                .findByTenant_IdAndDueAtGreaterThanEqualAndDueAtLessThan(
                                        context.tenantId(), from, to, pageable)
                        : projectTaskRepository
                                .findByTenant_IdAndProject_IdInAndDueAtGreaterThanEqualAndDueAtLessThan(
                                        context.tenantId(), readableProjectIds, from, to, pageable);

        Map<UUID, Boolean> projectAccess = new HashMap<>();
        return candidates.stream()
                .filter(
                        task ->
                                projectAccess.computeIfAbsent(
                                        task.getProject().getId(),
                                        projectId ->
                                                authorizationSecurity.canReadProjectTasks(
                                                        context.tenantId(),
                                                        projectId,
                                                        PlatformPermissionCodes.PROJECT_TASK_READ)))
                .map(
                        task ->
                                new CalendarDeadlineSnapshot(
                                        task.getId(),
                                        task.getProject().getId(),
                                        task.getTitle(),
                                        task.getProject().getName(),
                                        task.getStatus().name(),
                                        task.getPriority().name(),
                                        task.getDueAt()))
                .toList();
    }

    private Set<UUID> projectScopedIds(CalendarDeadlineContext context) {
        Set<UUID> projectIds = new LinkedHashSet<>();
        for (CalendarDeadlineContext.Grant grant : context.grants()) {
            if (grant.scopeType() == AuthorizationScopeType.PROJECT
                    && grant.scopeTargetId() != null
                    && grant.permissionCodes().contains(PlatformPermissionCodes.PROJECT_TASK_READ)) {
                projectIds.add(grant.scopeTargetId());
            }
        }
        return projectIds;
    }
}
