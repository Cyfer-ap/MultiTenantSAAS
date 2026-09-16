package com.chacha.multitenantsaas.tasks.risk;

import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.projectrisk.spi.ProjectRiskTaskSource;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TaskProjectRiskSource implements ProjectRiskTaskSource {

    private final ProjectTaskRepository taskRepository;

    public TaskProjectRiskSource(ProjectTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskSnapshot> findProjectTasks(UUID tenantId, UUID projectId, int requestedLimit) {
        int limit = Math.max(1, requestedLimit);
        var pageable =
                PageRequest.of(
                        0, limit, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        return taskRepository
                .findProjectTasks(tenantId, projectId, null, null, null, null, pageable)
                .stream()
                .map(this::snapshot)
                .toList();
    }

    private TaskSnapshot snapshot(ProjectTask task) {
        return new TaskSnapshot(
                task.getId(),
                task.getTitle(),
                task.getStatus().name(),
                task.getPriority().name(),
                task.getAssigneeUser() == null ? null : task.getAssigneeUser().getId(),
                task.getDueAt(),
                task.getUpdatedAt());
    }
}
