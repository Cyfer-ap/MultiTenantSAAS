package com.chacha.multitenantsaas.taskrelationships;

import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.taskrelationships.dto.TaskLabelResponse;
import com.chacha.multitenantsaas.taskrelationships.dto.TaskReferenceResponse;
import com.chacha.multitenantsaas.taskrelationships.dto.TaskRelationshipsResponse;
import com.chacha.multitenantsaas.taskrelationships.entity.ProjectTaskLabelAssignment;
import com.chacha.multitenantsaas.taskrelationships.entity.TaskDependency;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipTaskGateway;
import com.chacha.multitenantsaas.taskrelationships.repository.ProjectTaskLabelAssignmentRepository;
import com.chacha.multitenantsaas.taskrelationships.repository.TaskDependencyRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskRelationshipQueryService {

    static final int MAX_RELATED_TASKS = 200;

    private final TaskRelationshipTaskGateway taskGateway;
    private final TaskDependencyRepository dependencyRepository;
    private final ProjectTaskLabelAssignmentRepository assignmentRepository;

    public TaskRelationshipQueryService(
            TaskRelationshipTaskGateway taskGateway,
            TaskDependencyRepository dependencyRepository,
            ProjectTaskLabelAssignmentRepository assignmentRepository) {
        this.taskGateway = taskGateway;
        this.dependencyRepository = dependencyRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public TaskRelationshipsResponse getRelationships(UUID tenantId, UUID projectId, UUID taskId) {
        ProjectTask task = taskGateway.requireTask(tenantId, projectId, taskId);

        List<ProjectTask> children =
                taskGateway.findChildren(tenantId, projectId, taskId, MAX_RELATED_TASKS + 1);
        boolean childrenTruncated = children.size() > MAX_RELATED_TASKS;
        if (childrenTruncated) {
            children = children.subList(0, MAX_RELATED_TASKS);
        }

        var blockers =
                dependencyRepository
                        .findByTenant_IdAndProject_IdAndDependentTask_IdOrderByCreatedAtAsc(
                                tenantId,
                                projectId,
                                taskId,
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
                                taskId,
                                PageRequest.of(0, MAX_RELATED_TASKS + 1));
        boolean dependentsTruncated = dependents.size() > MAX_RELATED_TASKS;
        if (dependentsTruncated) {
            dependents = dependents.subList(0, MAX_RELATED_TASKS);
        }

        List<TaskLabelResponse> labels =
                assignmentRepository
                        .findByTenant_IdAndProject_IdAndTask_IdOrderByAssignedAtAsc(
                                tenantId, projectId, taskId)
                        .stream()
                        .map(ProjectTaskLabelAssignment::getLabel)
                        .map(
                                label ->
                                        new TaskLabelResponse(
                                                label.getId(), label.getName(), label.getColor()))
                        .toList();

        return new TaskRelationshipsResponse(
                task.getParentTask() == null ? null : toReference(task.getParentTask()),
                children.stream().map(this::toReference).toList(),
                blockers.stream()
                        .map(TaskDependency::getBlockingTask)
                        .map(this::toReference)
                        .toList(),
                dependents.stream()
                        .map(TaskDependency::getDependentTask)
                        .map(this::toReference)
                        .toList(),
                labels,
                childrenTruncated,
                blockersTruncated,
                dependentsTruncated);
    }

    private TaskReferenceResponse toReference(ProjectTask task) {
        return new TaskReferenceResponse(
                task.getId(), task.getTitle(), task.getStatus(), task.getPriority());
    }
}
