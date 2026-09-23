package com.chacha.multitenantsaas.externalaccess;

import com.chacha.multitenantsaas.taskcollaboration.external.ExternalTaskCommentCommand;
import com.chacha.multitenantsaas.taskcollaboration.external.ExternalTaskCommentPort;
import com.chacha.multitenantsaas.taskcollaboration.external.ExternalTaskCommentSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalPortalService {

    private final ExternalGuestSessionService sessionService;
    private final ExternalProjectProjectionPort projectProjectionPort;
    private final ExternalTaskProjectionPort taskProjectionPort;
    private final ExternalTaskCommentPort taskCommentPort;
    private final ExternalAccessProperties properties;

    public ExternalPortalService(
            ExternalGuestSessionService sessionService,
            ExternalProjectProjectionPort projectProjectionPort,
            ExternalTaskProjectionPort taskProjectionPort,
            ExternalTaskCommentPort taskCommentPort,
            ExternalAccessProperties properties) {
        this.sessionService = sessionService;
        this.projectProjectionPort = projectProjectionPort;
        this.taskProjectionPort = taskProjectionPort;
        this.taskCommentPort = taskCommentPort;
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

    @Transactional(readOnly = true)
    public ExternalAccessDtos.GuestCommentsResponse comments(String sessionToken, UUID taskId) {
        ExternalGuestSessionContext context = sessionService.requireSession(sessionToken);
        requireCommentCapability(context);
        int limit = properties.getCommentReadLimit();
        if (limit <= 0 || limit > 100) {
            throw new IllegalStateException(
                    "External comment read limit must be between 1 and 100");
        }

        List<ExternalAccessDtos.GuestCommentResponse> comments =
                taskCommentPort
                        .listGrantComments(
                                context.tenantId(),
                                context.projectId(),
                                taskId,
                                context.grantId(),
                                limit)
                        .stream()
                        .map(this::mapComment)
                        .toList();
        return new ExternalAccessDtos.GuestCommentsResponse(comments);
    }

    @Transactional
    public ExternalAccessDtos.GuestCommentResponse createComment(
            String sessionToken, UUID taskId, ExternalAccessDtos.GuestCommentRequest request) {
        ExternalGuestSessionContext context = sessionService.requireSession(sessionToken);
        requireCommentCapability(context);
        ExternalTaskCommentSnapshot comment =
                taskCommentPort.createComment(
                        new ExternalTaskCommentCommand(
                                context.tenantId(),
                                context.projectId(),
                                taskId,
                                context.grantId(),
                                context.guestName(),
                                context.guestEmail(),
                                request.body()));
        return mapComment(comment);
    }

    private void requireCommentCapability(ExternalGuestSessionContext context) {
        context.require(ExternalAccessCapability.TASK_READ);
        context.require(ExternalAccessCapability.TASK_COMMENT_CREATE);
    }

    private ExternalAccessDtos.GuestCommentResponse mapComment(
            ExternalTaskCommentSnapshot comment) {
        return new ExternalAccessDtos.GuestCommentResponse(
                comment.id(),
                comment.taskId(),
                comment.grantId(),
                comment.guestName(),
                comment.guestEmail(),
                comment.body(),
                comment.createdAt());
    }
}
