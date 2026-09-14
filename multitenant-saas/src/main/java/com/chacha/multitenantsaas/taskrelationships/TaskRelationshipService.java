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
import com.chacha.multitenantsaas.taskrelationships.dto.TaskReferenceResponse;
import com.chacha.multitenantsaas.taskrelationships.dto.TaskRelationshipsResponse;
import com.chacha.multitenantsaas.taskrelationships.entity.ProjectTaskLabel;
import com.chacha.multitenantsaas.taskrelationships.entity.ProjectTaskLabelAssignment;
import com.chacha.multitenantsaas.taskrelationships.entity.TaskDependency;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipChangeSink;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipTaskGateway;
import com.chacha.multitenantsaas.taskrelationships.repository.ProjectTaskLabelAssignmentRepository;
import com.chacha.multitenantsaas.taskrelationships.repository.ProjectTaskLabelRepository;
import com.chacha.multitenantsaas.taskrelationships.repository.TaskDependencyRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskRelationshipService {

    static final int MAX_PARENT_DEPTH = 64;
    static final int MAX_RELATED_TASKS = 200;
    static final int MAX_DEPENDENCY_EDGES = 1000;
    static final int MAX_LABELS_PER_TASK = 20;

    private final TaskRelationshipTaskGateway taskGateway;
    private final TaskDependencyRepository dependencyRepository;
    private final ProjectTaskLabelRepository labelRepository;
    private final ProjectTaskLabelAssignmentRepository assignmentRepository;
    private final TaskRelationshipChangeSink changeSink;

    public TaskRelationshipService(
            TaskRelationshipTaskGateway taskGateway,
            TaskDependencyRepository dependencyRepository,
            ProjectTaskLabelRepository labelRepository,
            ProjectTaskLabelAssignmentRepository assignmentRepository,
            TaskRelationshipChangeSink changeSink) {
        this.taskGateway = taskGateway;
        this.dependencyRepository = dependencyRepository;
        this.labelRepository = labelRepository;
        this.assignmentRepository = assignmentRepository;
        this.changeSink = changeSink;
    }

    @Transactional(readOnly = true)
    public TaskRelationshipsResponse getRelationships(UUID tenantId, UUID projectId, UUID taskId) {
        ProjectTask task = taskGateway.requireTask(tenantId, projectId, taskId);
        return buildResponse(tenantId, projectId, task);
    }

    @Transactional
    public TaskRelationshipsResponse updateParent(
            UUID tenantId, UUID projectId, UUID taskId, UUID parentTaskId, Jwt jwt) {
        Project project = requireModifiableProject(tenantId, projectId);
        ProjectTask task = taskGateway.requireTask(tenantId, projectId, taskId);
        ensureTaskMutable(task);

        UUID currentParentId =
                task.getParentTask() == null ? null : task.getParentTask().getId();
        if (java.util.Objects.equals(currentParentId, parentTaskId)) {
            return buildResponse(tenantId, projectId, task);
        }

        ProjectTask parent = null;
        if (parentTaskId != null) {
            if (taskId.equals(parentTaskId)) {
                throw new IllegalArgumentException("A task cannot be its own parent");
            }
            parent = taskGateway.requireTask(tenantId, projectId, parentTaskId);
            ensureTaskMutable(parent);
            ensureParentDoesNotCreateCycle(taskId, parent);
        }

        task.setParentTask(parent);
        ProjectTask saved = taskGateway.save(task);
        String summary =
                parent == null
                        ? "Removed task parent"
                        : "Set parent task to " + parent.getTitle() + " (" + parent.getId() + ")";
        changeSink.record(
                tenantId,
                project.getId(),
                taskId,
                jwt,
                AuditAction.TASK_PARENT_UPDATED,
                TaskActivityType.PARENT_CHANGED,
                summary);
        return buildResponse(tenantId, projectId, saved);
    }

    @Transactional
    public TaskRelationshipsResponse addDependency(
            UUID tenantId,
            UUID projectId,
            UUID dependentTaskId,
            UUID blockingTaskId,
            Jwt jwt) {
        Project project = requireModifiableProject(tenantId, projectId);
        ProjectTask dependent = taskGateway.requireTask(tenantId, projectId, dependentTaskId);
        ProjectTask blocking = taskGateway.requireTask(tenantId, projectId, blockingTaskId);
        ensureTaskMutable(dependent);
        ensureTaskMutable(blocking);

        if (dependentTaskId.equals(blockingTaskId)) {
            throw new IllegalArgumentException("A task cannot depend on itself");
        }

        if (dependencyRepository
                .existsByTenant_IdAndProject_IdAndBlockingTask_IdAndDependentTask_Id(
                        tenantId, projectId, blockingTaskId, dependentTaskId)) {
            return buildResponse(tenantId, projectId, dependent);
        }

        ensureDependencyDoesNotCreateCycle(tenantId, projectId, blockingTaskId, dependentTaskId);
        dependencyRepository.save(
                new TaskDependency(dependent.getTenant(), project, blocking, dependent));
        changeSink.record(
                tenantId,
                projectId,
                dependentTaskId,
                jwt,
                AuditAction.TASK_DEPENDENCY_UPDATED,
                TaskActivityType.DEPENDENCY_CHANGED,
                "Added blocker " + blocking.getTitle() + " (" + blockingTaskId + ")");
        return buildResponse(tenantId, projectId, dependent);
    }

    @Transactional
    public TaskRelationshipsResponse removeDependency(
            UUID tenantId,
            UUID projectId,
            UUID dependentTaskId,
            UUID blockingTaskId,
            Jwt jwt) {
        requireModifiableProject(tenantId, projectId);
        ProjectTask dependent = taskGateway.requireTask(tenantId, projectId, dependentTaskId);
        ensureTaskMutable(dependent);

        var dependency =
                dependencyRepository
                        .findByTenant_IdAndProject_IdAndBlockingTask_IdAndDependentTask_Id(
                                tenantId, projectId, blockingTaskId, dependentTaskId);
        if (dependency.isPresent()) {
            dependencyRepository.delete(dependency.get());
            changeSink.record(
                    tenantId,
                    projectId,
                    dependentTaskId,
                    jwt,
                    AuditAction.TASK_DEPENDENCY_UPDATED,
                    TaskActivityType.DEPENDENCY_CHANGED,
                    "Removed blocker " + blockingTaskId);
        }
        return buildResponse(tenantId, projectId, dependent);
    }

    @Transactional(readOnly = true)
    public List<TaskLabelResponse> listLabels(UUID tenantId, UUID projectId) {
        taskGateway.requireProject(tenantId, projectId);
        return labelRepository.findByTenant_IdAndProject_IdOrderByNameAsc(tenantId, projectId).stream()
                .map(this::toLabelResponse)
                .toList();
    }

    @Transactional
    public TaskLabelResponse createLabel(
            UUID tenantId, UUID projectId, ProjectTaskLabelRequest request, Jwt jwt) {
        Project project = requireModifiableProject(tenantId, projectId);
        String displayName = normalizeDisplayName(request.name());
        String normalizedName = normalizeLabelName(displayName);
        ensureLabelNameAvailable(tenantId, projectId, normalizedName, null);

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
        return toLabelResponse(label);
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
        ensureLabelNameAvailable(tenantId, projectId, normalizedName, labelId);

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
        return toLabelResponse(saved);
    }

    @Transactional
    public void deleteLabel(
            UUID tenantId, UUID projectId, UUID labelId, Jwt jwt) {
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
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            UUID labelId,
            Jwt jwt) {
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
        return buildResponse(tenantId, projectId, task);
    }

    @Transactional
    public TaskRelationshipsResponse unassignLabel(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            UUID labelId,
            Jwt jwt) {
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
        return buildResponse(tenantId, projectId, task);
    }

    private TaskRelationshipsResponse buildResponse(
            UUID tenantId, UUID projectId, ProjectTask task) {
        List<ProjectTask> children =
                taskGateway.findChildren(
                        tenantId, projectId, task.getId(), MAX_RELATED_TASKS + 1);
        boolean childrenTruncated = children.size() > MAX_RELATED_TASKS;
        if (childrenTruncated) {
            children = children.subList(0, MAX_RELATED_TASKS);
        }

        var blockers =
                dependencyRepository
                        .findByTenant_IdAndProject_IdAndDependentTask_IdOrderByCreatedAtAsc(
                                tenantId,
                                projectId,
                                task.getId(),
                                PageRequest.of(0, MAX_RELATED_TASKS + 1));
        boolean blockersTruncated = blockers.size() > MAX_RELATED_TASKS;
        if (blockersTruncated) {
            blockers = blockers.subList(0, MAX_RELATED_TASKS);
        }

        var dependents =
                dependencyRepository
                        .findByTenant_IdAndProject_IdAndBlockingTask_IdOrderByCreatedAtAsc(
                                tenantId,
                                projectId,
                                task.getId(),
                                PageRequest.of(0, MAX_RELATED_TASKS + 1));
        boolean dependentsTruncated = dependents.size() > MAX_RELATED_TASKS;
        if (dependentsTruncated) {
            dependents = dependents.subList(0, MAX_RELATED_TASKS);
        }

        List<TaskLabelResponse> labels =
                assignmentRepository
                        .findByTenant_IdAndProject_IdAndTask_IdOrderByAssignedAtAsc(
                                tenantId, projectId, task.getId())
                        .stream()
                        .map(ProjectTaskLabelAssignment::getLabel)
                        .map(this::toLabelResponse)
                        .toList();

        return new TaskRelationshipsResponse(
                task.getParentTask() == null ? null : toTaskReference(task.getParentTask()),
                children.stream().map(this::toTaskReference).toList(),
                blockers.stream()
                        .map(TaskDependency::getBlockingTask)
                        .map(this::toTaskReference)
                        .toList(),
                dependents.stream()
                        .map(TaskDependency::getDependentTask)
                        .map(this::toTaskReference)
                        .toList(),
                labels,
                childrenTruncated,
                blockersTruncated,
                dependentsTruncated);
    }

    private void ensureParentDoesNotCreateCycle(UUID childTaskId, ProjectTask parent) {
        Set<UUID> seen = new HashSet<>();
        ProjectTask cursor = parent;
        for (int depth = 0; depth < MAX_PARENT_DEPTH; depth++) {
            if (childTaskId.equals(cursor.getId())) {
                throw new IllegalArgumentException("Task parent would create a hierarchy cycle");
            }
            if (!seen.add(cursor.getId())) {
                throw new IllegalArgumentException("Existing task hierarchy contains a cycle");
            }
            if (cursor.getParentTask() == null) {
                return;
            }
            cursor = cursor.getParentTask();
        }
        throw new IllegalArgumentException(
                "Task hierarchy exceeds the maximum supported depth of " + MAX_PARENT_DEPTH);
    }

    private void ensureDependencyDoesNotCreateCycle(
            UUID tenantId, UUID projectId, UUID blockingTaskId, UUID dependentTaskId) {
        List<TaskDependency> edges =
                dependencyRepository.findByTenant_IdAndProject_Id(
                        tenantId, projectId, PageRequest.of(0, MAX_DEPENDENCY_EDGES + 1));
        if (edges.size() > MAX_DEPENDENCY_EDGES) {
            throw new IllegalArgumentException(
                    "Project dependency graph exceeds the supported edge limit of "
                            + MAX_DEPENDENCY_EDGES);
        }

        Map<UUID, List<UUID>> adjacency = new HashMap<>();
        for (TaskDependency edge : edges) {
            adjacency
                    .computeIfAbsent(edge.getBlockingTask().getId(), ignored -> new ArrayList<>())
                    .add(edge.getDependentTask().getId());
        }

        ArrayDeque<UUID> queue = new ArrayDeque<>();
        Set<UUID> visited = new HashSet<>();
        queue.add(dependentTaskId);
        while (!queue.isEmpty()) {
            UUID current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            if (blockingTaskId.equals(current)) {
                throw new IllegalArgumentException("Task dependency would create a directed cycle");
            }
            if (visited.size() > MAX_DEPENDENCY_EDGES + 1) {
                throw new IllegalArgumentException("Dependency traversal exceeded its safety bound");
            }
            queue.addAll(adjacency.getOrDefault(current, List.of()));
        }
    }

    private Project requireModifiableProject(UUID tenantId, UUID projectId) {
        Project project = taskGateway.requireProject(tenantId, projectId);
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException("Archived project task relationships cannot be modified");
        }
        return project;
    }

    private void ensureTaskMutable(ProjectTask task) {
        if (task.getStatus() == ProjectTaskStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled task relationships cannot be modified");
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

    private void ensureLabelNameAvailable(
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

    private TaskReferenceResponse toTaskReference(ProjectTask task) {
        return new TaskReferenceResponse(
                task.getId(), task.getTitle(), task.getStatus(), task.getPriority());
    }

    private TaskLabelResponse toLabelResponse(ProjectTaskLabel label) {
        return new TaskLabelResponse(label.getId(), label.getName(), label.getColor());
    }
}
