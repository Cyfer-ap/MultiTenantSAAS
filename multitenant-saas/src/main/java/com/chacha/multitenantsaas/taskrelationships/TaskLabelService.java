package com.chacha.multitenantsaas.taskrelationships;

import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.entity.TaskActivityType;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.taskrelationships.dto.ProjectTaskLabelRequest;
import com.chacha.multitenantsaas.taskrelationships.dto.TaskLabelResponse;
import com.chacha.multitenantsaas.taskrelationships.dto.TaskRelationshipsResponse;
import com.chacha.multitenantsaas.taskrelationships.entity.ProjectTaskLabel;
import com.chacha.multitenantsaas.taskrelationships.entity.ProjectTaskLabelAssignment;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipChangeSink;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipTaskGateway;
import com.chacha.multitenantsaas.taskrelationships.repository.ProjectTaskLabelAssignmentRepository;
import com.chacha.multitenantsaas.taskrelationships.repository.ProjectTaskLabelRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskLabelService {

    static final int MAX_LABELS_PER_PROJECT = 200;
    static final int MAX_LABELS_PER_TASK = 20;

    private final TaskRelationshipTaskGateway taskGateway;
    private final ProjectTaskLabelRepository labelRepository;
    private final ProjectTaskLabelAssignmentRepository assignmentRepository;
    private final TaskRelationshipChangeSink changeSink;
    private final TaskRelationshipQueryService queryService;

    public TaskLabelService(
            TaskRelationshipTaskGateway taskGateway,
            ProjectTaskLabelRepository labelRepository,
            ProjectTaskLabelAssignmentRepository assignmentRepository,
            TaskRelationshipChangeSink changeSink,
            TaskRelationshipQueryService queryService) {
        this.taskGateway = taskGateway;
        this.labelRepository = labelRepository;
        this.assignmentRepository = assignmentRepository;
        this.changeSink = changeSink;
        this.queryService = queryService;
    }

    @Transactional(readOnly = true)
    public List<TaskLabelResponse> listLabels(UUID tenantId, UUID projectId) {
        taskGateway.requireProject(tenantId, projectId);
        return labelRepository
                .findByTenant_IdAndProject_IdOrderByNameAsc(
                        tenantId, projectId, PageRequest.of(0, MAX_LABELS_PER_PROJECT))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskLabelResponse createLabel(
            UUID tenantId, UUID projectId, ProjectTaskLabelRequest request, Jwt jwt) {
        Project project = requireModifiableProject(tenantId, projectId);
        if (labelRepository.countByTenant_IdAndProject_Id(tenantId, projectId)
                >= MAX_LABELS_PER_PROJECT) {
            throw new IllegalArgumentException(
                    "A project can have at most " + MAX_LABELS_PER_PROJECT + " task labels");
        }

        String displayName = normalizeDisplayName(request.name());
        String normalizedName = normalizeLabelName(displayName);
        ensureNameAvailable(tenantId, projectId, normalizedName, null);

        ProjectTaskLabel label =
                labelRepository.save(
                        new ProjectTaskLabel(
                                project.getTenant(),
                                project,
                                displayName,
                                normalizedName,
                                normalizeColor(request.color())));
        changeSink.record(
                tenantId,
                projectId,
                null,
                jwt,
                AuditAction.PROJECT_TASK_LABEL_CREATED,
                null,
                "Created project task label " + displayName);
        return toResponse(label);
    }

    @Transactional
    public TaskLabelResponse updateLabel(
            UUID tenantId,
            UUID projectId,
            UUID labelId,
            ProjectTaskLabelRequest request,
            Jwt jwt) {
        requireModifiableProject(tenantId, projectId);
        ProjectTaskLabel label = requireLabel(tenantId, projectId, labelId);
        String displayName = normalizeDisplayName(request.name());
        String normalizedName = normalizeLabelName(displayName);
        ensureNameAvailable(tenantId, projectId, normalizedName, labelId);

        label.setName(displayName);
        label.setNormalizedName(normalizedName);
        label.setColor(normalizeColor(request.color()));
        ProjectTaskLabel saved = labelRepository.save(label);
        changeSink.record(
                tenantId,
                projectId,
                null,
                jwt,
                AuditAction.PROJECT_TASK_LABEL_UPDATED,
                null,
                "Updated project task label " + displayName);
        return toResponse(saved);
    }

    @Transactional
    public void deleteLabel(UUID tenantId, UUID projectId, UUID labelId, Jwt jwt) {
        requireModifiableProject(tenantId, projectId);
        ProjectTaskLabel label = requireLabel(tenantId, projectId, labelId);
        assignmentRepository.deleteByTenant_IdAndProject_IdAndLabel_Id(
                tenantId, projectId, labelId);
        labelRepository.delete(label);
        changeSink.record(
                tenantId,
                projectId,
                null,
                jwt,
                AuditAction.PROJECT_TASK_LABEL_DELETED,
                null,
                "Deleted project task label " + label.getName());
    }

    @Transactional
    public TaskRelationshipsResponse assignLabel(
            UUID tenantId, UUID projectId, UUID taskId, UUID labelId, Jwt jwt) {
        Project project = requireModifiableProject(tenantId, projectId);
        ProjectTask task = taskGateway.requireTask(tenantId, projectId, taskId);
        ensureTaskMutable(task);
        ProjectTaskLabel label = requireLabel(tenantId, projectId, labelId);

        if (assignmentRepository
                .findByTenant_IdAndProject_IdAndTask_IdAndLabel_Id(
                        tenantId, projectId, taskId, labelId)
                .isEmpty()) {
            long currentCount =
                    assignmentRepository.countByTenant_IdAndProject_IdAndTask_Id(
                            tenantId, projectId, taskId);
            if (currentCount >= MAX_LABELS_PER_TASK) {
                throw new IllegalArgumentException(
                        "A task can have at most " + MAX_LABELS_PER_TASK + " labels");
            }
            assignmentRepository.save(
                    new ProjectTaskLabelAssignment(task.getTenant(), project, task, label));
            changeSink.record(
                    tenantId,
                    projectId,
                    taskId,
                    jwt,
                    AuditAction.TASK_LABEL_UPDATED,
                    TaskActivityType.LABELS_CHANGED,
                    "Added label " + label.getName());
        }
        return queryService.getRelationships(tenantId, projectId, taskId);
    }

    @Transactional
    public TaskRelationshipsResponse unassignLabel(
            UUID tenantId, UUID projectId, UUID taskId, UUID labelId, Jwt jwt) {
        requireModifiableProject(tenantId, projectId);
        ProjectTask task = taskGateway.requireTask(tenantId, projectId, taskId);
        ensureTaskMutable(task);

        var assignment =
                assignmentRepository.findByTenant_IdAndProject_IdAndTask_IdAndLabel_Id(
                        tenantId, projectId, taskId, labelId);
        if (assignment.isPresent()) {
            assignmentRepository.delete(assignment.get());
            changeSink.record(
                    tenantId,
                    projectId,
                    taskId,
                    jwt,
                    AuditAction.TASK_LABEL_UPDATED,
                    TaskActivityType.LABELS_CHANGED,
                    "Removed label " + labelId);
        }
        return queryService.getRelationships(tenantId, projectId, taskId);
    }

    private Project requireModifiableProject(UUID tenantId, UUID projectId) {
        Project project = taskGateway.requireProject(tenantId, projectId);
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException("Archived project task labels cannot be modified");
        }
        return project;
    }

    private void ensureTaskMutable(ProjectTask task) {
        if (task.getStatus() == ProjectTaskStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled task labels cannot be modified");
        }
    }

    private ProjectTaskLabel requireLabel(UUID tenantId, UUID projectId, UUID labelId) {
        return labelRepository
                .findByTenant_IdAndProject_IdAndId(tenantId, projectId, labelId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Task label not found with id: " + labelId));
    }

    private void ensureNameAvailable(
            UUID tenantId, UUID projectId, String normalizedName, UUID excludedLabelId) {
        boolean exists =
                excludedLabelId == null
                        ? labelRepository.existsByTenant_IdAndProject_IdAndNormalizedName(
                                tenantId, projectId, normalizedName)
                        : labelRepository
                                .existsByTenant_IdAndProject_IdAndNormalizedNameAndIdNot(
                                        tenantId, projectId, normalizedName, excludedLabelId);
        if (exists) {
            throw new IllegalArgumentException("A label with this name already exists in the project");
        }
    }

    private String normalizeDisplayName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private String normalizeLabelName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private String normalizeColor(String color) {
        return color == null || color.isBlank() ? null : color.toUpperCase(Locale.ROOT);
    }

    private TaskLabelResponse toResponse(ProjectTaskLabel label) {
        return new TaskLabelResponse(label.getId(), label.getName(), label.getColor());
    }
}
