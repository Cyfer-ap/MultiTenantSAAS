package com.chacha.multitenantsaas.taskcollaboration.external;

import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.entity.TaskComment;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.repository.TaskCommentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultExternalTaskCommentAdapter implements ExternalTaskCommentPort {

    private static final int MAX_BODY_LENGTH = 4000;
    private static final int MAX_GUEST_NAME_LENGTH = 150;
    private static final int MAX_GUEST_EMAIL_LENGTH = 150;

    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final TaskCommentRepository commentRepository;

    public DefaultExternalTaskCommentAdapter(
            ProjectRepository projectRepository,
            ProjectTaskRepository taskRepository,
            TaskCommentRepository commentRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    @Transactional
    public ExternalTaskCommentSnapshot createComment(ExternalTaskCommentCommand command) {
        Project project = requireProject(command.tenantId(), command.projectId());
        ProjectTask task = requireTask(command.tenantId(), command.projectId(), command.taskId());
        requireMutable(project, task);

        String body = normalizeBody(command.body());
        String guestName = normalizeRequired(command.guestName(), MAX_GUEST_NAME_LENGTH, "Guest name");
        String guestEmail =
                normalizeRequired(command.guestEmail(), MAX_GUEST_EMAIL_LENGTH, "Guest email");

        TaskComment saved =
                commentRepository.saveAndFlush(
                        new TaskComment(
                                project.getTenant(),
                                project,
                                task,
                                command.grantId(),
                                guestName,
                                guestEmail,
                                body));
        return map(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalTaskCommentSnapshot> listGrantComments(
            UUID tenantId, UUID projectId, UUID taskId, UUID grantId, int limit) {
        requireProject(tenantId, projectId);
        requireTask(tenantId, projectId, taskId);
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("External comment read limit must be between 1 and 100");
        }

        return commentRepository
                .findByTenant_IdAndProject_IdAndTask_IdAndExternalAccessGrantIdAndParentCommentIsNullAndDeletedFalse(
                        tenantId,
                        projectId,
                        taskId,
                        grantId,
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "createdAt")))
                .getContent()
                .stream()
                .map(this::map)
                .toList();
    }

    private Project requireProject(UUID tenantId, UUID projectId) {
        return projectRepository
                .findByTenant_IdAndId(tenantId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private ProjectTask requireTask(UUID tenantId, UUID projectId, UUID taskId) {
        return taskRepository
                .findByProject_Tenant_IdAndProject_IdAndId(tenantId, projectId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private void requireMutable(Project project, ProjectTask task) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException("Archived project collaboration is read-only");
        }
        if (task.getStatus() == ProjectTaskStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled task collaboration is read-only");
        }
    }

    private String normalizeBody(String value) {
        String normalized = normalizeRequired(value, MAX_BODY_LENGTH, "Comment body");
        if (normalized.length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("Comment body cannot exceed 4000 characters");
        }
        return normalized;
    }

    private String normalizeRequired(String value, int maxLength, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required");
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " is too long");
        }
        return normalized;
    }

    private ExternalTaskCommentSnapshot map(TaskComment comment) {
        return new ExternalTaskCommentSnapshot(
                comment.getId(),
                comment.getTask().getId(),
                comment.getExternalAccessGrantId(),
                comment.getExternalGuestName(),
                comment.getExternalGuestEmail(),
                comment.getBody(),
                comment.getCreatedAt());
    }
}
