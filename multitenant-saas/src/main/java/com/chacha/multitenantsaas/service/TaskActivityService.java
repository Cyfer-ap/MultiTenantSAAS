package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.TaskActivityResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.TaskActivity;
import com.chacha.multitenantsaas.entity.TaskActivityType;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.repository.TaskActivityRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskActivityService {

    private final TaskActivityRepository taskActivityRepository;
    private final ProjectTaskRepository projectTaskRepository;

    public TaskActivityService(
            TaskActivityRepository taskActivityRepository,
            ProjectTaskRepository projectTaskRepository) {
        this.taskActivityRepository = taskActivityRepository;
        this.projectTaskRepository = projectTaskRepository;
    }

    @Transactional
    public void record(
            ProjectTask task, AppUser actor, TaskActivityType activityType, String summary) {
        TaskActivity activity =
                new TaskActivity(
                        task.getTenant(),
                        task.getProject(),
                        task,
                        actor,
                        activityType,
                        summary);
        taskActivityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskActivityResponse> getActivities(
            UUID tenantId, UUID projectId, UUID taskId, Pageable pageable) {
        ensureTaskExists(tenantId, projectId, taskId);

        Page<TaskActivity> activities =
                taskActivityRepository.findByTenant_IdAndProject_IdAndTask_Id(
                        tenantId, projectId, taskId, pageable);

        return new PageResponse<>(
                activities.getContent().stream().map(this::mapToResponse).toList(),
                activities.getNumber(),
                activities.getSize(),
                activities.getTotalElements(),
                activities.getTotalPages(),
                activities.isFirst(),
                activities.isLast());
    }

    private void ensureTaskExists(UUID tenantId, UUID projectId, UUID taskId) {
        if (!projectTaskRepository
                .findByProject_Tenant_IdAndProject_IdAndId(tenantId, projectId, taskId)
                .isPresent()) {
            throw new ResourceNotFoundException(
                    "Task not found with id: " + taskId + " for project: " + projectId);
        }
    }

    private TaskActivityResponse mapToResponse(TaskActivity activity) {
        AppUser actor = activity.getActorUser();

        return new TaskActivityResponse(
                activity.getId(),
                activity.getTask().getId(),
                activity.getActivityType(),
                actor.getId(),
                actor.getFullName(),
                actor.getEmail(),
                activity.getSummary(),
                activity.getCreatedAt());
    }
}
