package com.chacha.multitenantsaas.tasks.automation;

import com.chacha.multitenantsaas.dto.ProjectTaskResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectMemberRole;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.entity.TaskActivityType;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectMemberRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.security.AuthorizationEvaluationContext;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.service.AuditLogService;
import com.chacha.multitenantsaas.service.AuthorizationPermissionEvaluator;
import com.chacha.multitenantsaas.service.OutboundWebhookEventService;
import com.chacha.multitenantsaas.service.TaskActivityService;
import com.chacha.multitenantsaas.service.TaskNotificationService;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultTaskAutomationMutationAdapter implements TaskAutomationMutationPort {

    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final AppUserRepository appUserRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuthorizationPermissionEvaluator permissionEvaluator;
    private final TaskActivityService taskActivityService;
    private final AuditLogService auditLogService;
    private final TaskNotificationService taskNotificationService;
    private final OutboundWebhookEventService outboundWebhookEventService;

    public DefaultTaskAutomationMutationAdapter(
            ProjectRepository projectRepository,
            ProjectTaskRepository taskRepository,
            AppUserRepository appUserRepository,
            ProjectMemberRepository projectMemberRepository,
            AuthorizationPermissionEvaluator permissionEvaluator,
            TaskActivityService taskActivityService,
            AuditLogService auditLogService,
            TaskNotificationService taskNotificationService,
            OutboundWebhookEventService outboundWebhookEventService) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.appUserRepository = appUserRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.permissionEvaluator = permissionEvaluator;
        this.taskActivityService = taskActivityService;
        this.auditLogService = auditLogService;
        this.taskNotificationService = taskNotificationService;
        this.outboundWebhookEventService = outboundWebhookEventService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TaskAutomationSnapshot mutate(TaskAutomationMutationCommand command) {
        Project project =
                projectRepository
                        .findByTenant_IdAndId(command.tenantId(), command.projectId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Project not found: " + command.projectId()));
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Archived project tasks cannot be automated");
        }

        ProjectTask task =
                taskRepository
                        .findByProject_Tenant_IdAndProject_IdAndId(
                                command.tenantId(), command.projectId(), command.taskId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Task not found: " + command.taskId()));
        if (task.getStatus() == ProjectTaskStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled task cannot be automated");
        }

        AppUser actor =
                appUserRepository
                        .findByTenantIdAndId(command.tenantId(), command.actorUserId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Workflow actor not found: "
                                                        + command.actorUserId()));
        if (actor.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Workflow actor is no longer active");
        }

        return switch (command.mutationType()) {
            case SET_PRIORITY -> setPriority(command, project, task, actor);
            case SET_STATUS -> setStatus(command, project, task, actor);
        };
    }

    private TaskAutomationSnapshot setPriority(
            TaskAutomationMutationCommand command,
            Project project,
            ProjectTask task,
            AppUser actor) {
        requireManageAuthority(command, task, false);
        ProjectTaskPriority priority = ProjectTaskPriority.valueOf(command.value());
        if (task.getPriority() == priority) {
            return snapshot(task);
        }

        ProjectTaskPriority previous = task.getPriority();
        task.setPriority(priority);
        ProjectTask saved = taskRepository.save(task);
        String summary =
                "Workflow "
                        + command.workflowId()
                        + " set priority from "
                        + previous
                        + " to "
                        + priority;
        taskActivityService.record(saved, actor, TaskActivityType.TASK_UPDATED, summary);
        auditLogService.recordSuccess(
                project.getTenant(),
                actor,
                task.getAssigneeUser() != null ? task.getAssigneeUser() : actor,
                AuditAction.TASK_UPDATED,
                summary + " (execution " + command.workflowExecutionId() + ")");
        outboundWebhookEventService.publish(
                command.tenantId(), OutboundWebhookEventType.TASK_UPDATED, mapResponse(saved));
        return snapshot(saved);
    }

    private TaskAutomationSnapshot setStatus(
            TaskAutomationMutationCommand command,
            Project project,
            ProjectTask task,
            AppUser actor) {
        requireManageAuthority(command, task, true);
        ProjectTaskStatus status = ProjectTaskStatus.valueOf(command.value());
        if (status == ProjectTaskStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Workflow actions cannot cancel tasks; cancellation requires the dedicated operation");
        }
        if (task.getStatus() == status) {
            return snapshot(task);
        }

        ProjectTaskStatus previous = task.getStatus();
        task.setStatus(status);
        task.setCompletedAt(status == ProjectTaskStatus.COMPLETED ? Instant.now() : null);
        ProjectTask saved = taskRepository.save(task);
        String summary =
                "Workflow "
                        + command.workflowId()
                        + " changed status from "
                        + previous
                        + " to "
                        + status;
        taskActivityService.record(saved, actor, TaskActivityType.STATUS_CHANGED, summary);
        auditLogService.recordSuccess(
                project.getTenant(),
                actor,
                task.getAssigneeUser() != null ? task.getAssigneeUser() : actor,
                AuditAction.TASK_STATUS_UPDATED,
                summary + " (execution " + command.workflowExecutionId() + ")");
        taskNotificationService.notifyStatusChanged(saved, actor, previous, status);
        outboundWebhookEventService.publish(
                command.tenantId(),
                status == ProjectTaskStatus.COMPLETED
                        ? OutboundWebhookEventType.TASK_COMPLETED
                        : OutboundWebhookEventType.TASK_UPDATED,
                mapResponse(saved));
        return snapshot(saved);
    }

    private void requireManageAuthority(
            TaskAutomationMutationCommand command, ProjectTask task, boolean allowAssignee) {
        boolean permission =
                permissionEvaluator.hasPermission(
                        command.tenantId(),
                        command.actorUserId(),
                        PlatformPermissionCodes.PROJECT_TASK_MANAGE,
                        AuthorizationEvaluationContext.project(command.projectId()));
        if (permission) {
            return;
        }

        boolean projectLead =
                projectMemberRepository
                        .findByProject_Tenant_IdAndProject_IdAndUser_Id(
                                command.tenantId(), command.projectId(), command.actorUserId())
                        .map(member -> member.getRole() == ProjectMemberRole.PROJECT_LEAD)
                        .orElse(false);
        if (projectLead) {
            return;
        }

        boolean assignee =
                allowAssignee
                        && task.getAssigneeUser() != null
                        && command.actorUserId().equals(task.getAssigneeUser().getId());
        if (!assignee) {
            throw new AccessDeniedException(
                    "Workflow actor no longer has authority to mutate this task");
        }
    }

    private TaskAutomationSnapshot snapshot(ProjectTask task) {
        return new TaskAutomationSnapshot(task.getStatus(), task.getPriority());
    }

    private ProjectTaskResponse mapResponse(ProjectTask task) {
        AppUser assignee = task.getAssigneeUser();
        AppUser creator = task.getCreatedByUser();
        return new ProjectTaskResponse(
                task.getId(),
                task.getTenant().getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                assignee != null ? assignee.getId() : null,
                assignee != null ? assignee.getFullName() : null,
                assignee != null ? assignee.getEmail() : null,
                creator.getId(),
                creator.getFullName(),
                creator.getEmail(),
                task.getDueAt(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
