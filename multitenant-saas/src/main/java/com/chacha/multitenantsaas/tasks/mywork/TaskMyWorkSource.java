package com.chacha.multitenantsaas.tasks.mywork;

import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.mywork.spi.MyWorkTaskSnapshot;
import com.chacha.multitenantsaas.mywork.spi.MyWorkTaskSource;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class TaskMyWorkSource implements MyWorkTaskSource {

    private static final int MAX_FETCH = 400;
    private static final List<ProjectTaskStatus> CLOSED_STATUSES =
            List.of(ProjectTaskStatus.COMPLETED, ProjectTaskStatus.CANCELLED);

    private final ProjectTaskRepository projectTaskRepository;
    private final AuthorizationSecurityService authorizationSecurity;

    public TaskMyWorkSource(
            ProjectTaskRepository projectTaskRepository,
            AuthorizationSecurityService authorizationSecurity) {
        this.projectTaskRepository = projectTaskRepository;
        this.authorizationSecurity = authorizationSecurity;
    }

    @Override
    public List<MyWorkTaskSnapshot> findAssignedOpenTasks(
            UUID tenantId, UUID userId, int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, MAX_FETCH));
        var pageable =
                PageRequest.of(
                        0,
                        limit,
                        Sort.by(
                                Sort.Order.asc("dueAt").nullsLast(),
                                Sort.Order.desc("updatedAt")));
        Map<UUID, Boolean> projectAccess = new HashMap<>();

        return projectTaskRepository
                .findByTenant_IdAndAssigneeUser_IdAndStatusNotIn(
                        tenantId, userId, CLOSED_STATUSES, pageable)
                .stream()
                .filter(
                        task ->
                                projectAccess.computeIfAbsent(
                                        task.getProject().getId(),
                                        projectId ->
                                                authorizationSecurity.canReadProjectTasks(
                                                        tenantId,
                                                        projectId,
                                                        PlatformPermissionCodes.PROJECT_TASK_READ)))
                .map(
                        task ->
                                new MyWorkTaskSnapshot(
                                        task.getId(),
                                        task.getProject().getId(),
                                        task.getTitle(),
                                        task.getProject().getName(),
                                        task.getStatus().name(),
                                        task.getPriority().name(),
                                        task.getDueAt(),
                                        task.getUpdatedAt()))
                .toList();
    }
}
