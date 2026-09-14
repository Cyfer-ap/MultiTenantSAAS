package com.chacha.multitenantsaas.tasks.creation;

import com.chacha.multitenantsaas.dto.ProjectTaskResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.TaskActivityType;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectMemberRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.service.AuditLogService;
import com.chacha.multitenantsaas.service.OutboundWebhookEventService;
import com.chacha.multitenantsaas.service.TaskActivityService;
import com.chacha.multitenantsaas.service.TaskNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultTaskCreationAdapter implements TaskCreationPort {

    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final AppUserRepository appUserRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskActivityService taskActivityService;
    private final AuditLogService auditLogService;
    private final TaskNotificationService taskNotificationService;
    private final OutboundWebhookEventService outboundWebhookEventService;

    public DefaultTaskCreationAdapter(
            ProjectRepository projectRepository,
            ProjectTaskRepository projectTaskRepository,
            AppUserRepository appUserRepository,
            ProjectMemberRepository projectMemberRepository,
            TaskActivityService taskActivityService,
            AuditLogService auditLogService,
            TaskNotificationService taskNotificationService,
            OutboundWebhookEventService outboundWebhookEventService) {
        this.projectRepository = projectRepository;
        this.projectTaskRepository = projectTaskRepository;
        this.appUserRepository = appUserRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskActivityService = taskActivityService;
        this.auditLogService = auditLogService;
        this.taskNotificationService = taskNotificationService;
        this.outboundWebhookEventService = outboundWebhookEventService;
    }

    @Override
    @Transactional
    public TaskCreationResult createTask(TaskCreationCommand command) {
        Project project =
                projectRepository
                        .findByTenant_IdAndId(command.tenantId(), command.projectId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Project not found: " + command.projectId()));
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Archived projects cannot receive generated tasks");
        }

        AppUser creator = requireActiveUser(command.tenantId(), command.creatorUserId(), "creator");
        AppUser assignee = null;
        if (command.assigneeUserId() != null) {
            assignee = requireActiveUser(command.tenantId(), command.assigneeUserId(), "assignee");
            if (!projectMemberRepository.existsByProject_Tenant_IdAndProject_IdAndUser_Id(
                    command.tenantId(), command.projectId(), assignee.getId())) {
                throw new IllegalStateException(
                        "Generated-task assignee is no longer a project member");
            }
        }

        ProjectTask task =
                new ProjectTask(
                        project.getTenant(),
                        project,
                        creator,
                        assignee,
                        command.title().trim(),
                        normalizeDescription(command.description()),
                        command.priority(),
                        command.dueAt());
        ProjectTask saved = projectTaskRepository.save(task);

        String activitySummary =
                command.activitySummary() == null || command.activitySummary().isBlank()
                        ? "Task created"
                        : command.activitySummary().trim();
        taskActivityService.record(saved, creator, TaskActivityType.TASK_CREATED, activitySummary);
        auditLogService.recordSuccess(
                project.getTenant(),
                creator,
                assignee != null ? assignee : creator,
                AuditAction.TASK_CREATED,
                activitySummary + ": " + saved.getId() + " - " + saved.getTitle());
        taskNotificationService.notifyAssignment(saved, creator, assignee);
        outboundWebhookEventService.publish(
                command.tenantId(), OutboundWebhookEventType.TASK_CREATED, mapResponse(saved));

        return new TaskCreationResult(saved.getId(), saved.getCreatedAt());
    }

    private AppUser requireActiveUser(java.util.UUID tenantId, java.util.UUID userId, String role) {
        AppUser user =
                appUserRepository
                        .findByTenantIdAndId(tenantId, userId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Generated-task "
                                                        + role
                                                        + " not found: "
                                                        + userId));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Generated-task " + role + " is not active");
        }
        return user;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
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
