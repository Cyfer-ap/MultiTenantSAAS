package com.chacha.multitenantsaas.externalaccess;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalPortalService {

    private final ExternalGuestSessionService sessionService;
    private final ExternalProjectProjectionPort projectProjectionPort;
    private final ExternalTaskProjectionPort taskProjectionPort;
    private final ExternalAccessProperties properties;

    public ExternalPortalService(
            ExternalGuestSessionService sessionService,
            ExternalProjectProjectionPort projectProjectionPort,
            ExternalTaskProjectionPort taskProjectionPort,
            ExternalAccessProperties properties) {
        this.sessionService = sessionService;
        this.projectProjectionPort = projectProjectionPort;
        this.taskProjectionPort = taskProjectionPort;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public ExternalAccessDtos.SessionResponse session(String sessionToken) {
        ExternalGuestSessionContext context = sessionService.requireSession(sessionToken);
        context.require(ExternalAccessCapability.PROJECT_READ);
        ExternalProjectSnapshot project =
                projectProjectionPort.requireProject(context.tenantId(), context.projectId());

        return new ExternalAccessDtos.SessionResponse(
                context.grantId(),
                context.guestName(),
                context.guestEmail(),
                context.capabilities(),
                context.grantExpiresAt(),
                context.sessionExpiresAt(),
                new ExternalAccessDtos.ProjectResponse(
                        project.projectId(),
                        project.name(),
                        project.description(),
                        project.status(),
                        project.updatedAt()));
    }

    @Transactional(readOnly = true)
    public ExternalAccessDtos.TasksResponse tasks(String sessionToken) {
        ExternalGuestSessionContext context = sessionService.requireSession(sessionToken);
        context.require(ExternalAccessCapability.TASK_READ);
        int limit = properties.getTaskReadLimit();
        if (limit <= 0 || limit > 100) {
            throw new IllegalStateException("External task read limit must be between 1 and 100");
        }

        List<ExternalAccessDtos.TaskResponse> tasks =
                taskProjectionPort
                        .listTasks(context.tenantId(), context.projectId(), limit)
                        .stream()
                        .map(
                                task ->
                                        new ExternalAccessDtos.TaskResponse(
                                                task.taskId(),
                                                task.title(),
                                                task.description(),
                                                task.status(),
                                                task.priority(),
                                                task.dueAt(),
                                                task.completedAt(),
                                                task.updatedAt()))
                        .toList();
        return new ExternalAccessDtos.TasksResponse(tasks);
    }
}
