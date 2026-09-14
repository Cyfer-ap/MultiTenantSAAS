package com.chacha.multitenantsaas.taskrelationships;

import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.entity.TaskActivityType;
import com.chacha.multitenantsaas.taskrelationships.dto.TaskRelationshipsResponse;
import com.chacha.multitenantsaas.taskrelationships.entity.TaskDependency;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipChangeSink;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipTaskGateway;
import com.chacha.multitenantsaas.taskrelationships.repository.TaskDependencyRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskGraphService {

    static final int MAX_PARENT_DEPTH = 64;
    static final int MAX_DEPENDENCY_EDGES = 1000;

    private final TaskRelationshipTaskGateway taskGateway;
    private final TaskDependencyRepository dependencyRepository;
    private final TaskRelationshipChangeSink changeSink;
    private final TaskRelationshipQueryService queryService;

    public TaskGraphService(
            TaskRelationshipTaskGateway taskGateway,
            TaskDependencyRepository dependencyRepository,
            TaskRelationshipChangeSink changeSink,
            TaskRelationshipQueryService queryService) {
        this.taskGateway = taskGateway;
        this.dependencyRepository = dependencyRepository;
        this.changeSink = changeSink;
        this.queryService = queryService;
    }

    @Transactional
    public TaskRelationshipsResponse updateParent(
            UUID tenantId, UUID projectId, UUID taskId, UUID parentTaskId, Jwt jwt) {
        Project project = requireModifiableProject(tenantId, projectId);
        ProjectTask task = taskGateway.requireTask(tenantId, projectId, taskId);
        ensureTaskMutable(task);

        UUID currentParentId = task.getParentTask() == null ? null : task.getParentTask().getId();
        if (java.util.Objects.equals(currentParentId, parentTaskId)) {
            return queryService.getRelationships(tenantId, projectId, taskId);
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
        taskGateway.save(task);
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
        return queryService.getRelationships(tenantId, projectId, taskId);
    }

    @Transactional
    public TaskRelationshipsResponse addDependency(
            UUID tenantId, UUID projectId, UUID dependentTaskId, UUID blockingTaskId, Jwt jwt) {
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
            return queryService.getRelationships(tenantId, projectId, dependentTaskId);
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
        return queryService.getRelationships(tenantId, projectId, dependentTaskId);
    }

    @Transactional
    public TaskRelationshipsResponse removeDependency(
            UUID tenantId, UUID projectId, UUID dependentTaskId, UUID blockingTaskId, Jwt jwt) {
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
        return queryService.getRelationships(tenantId, projectId, dependentTaskId);
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
                throw new IllegalArgumentException(
                        "Dependency traversal exceeded its safety bound");
            }
            queue.addAll(adjacency.getOrDefault(current, List.of()));
        }
    }

    private Project requireModifiableProject(UUID tenantId, UUID projectId) {
        Project project = taskGateway.requireProject(tenantId, projectId);
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "Archived project task relationships cannot be modified");
        }
        return project;
    }

    private void ensureTaskMutable(ProjectTask task) {
        if (task.getStatus() == ProjectTaskStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled task relationships cannot be modified");
        }
    }
}
