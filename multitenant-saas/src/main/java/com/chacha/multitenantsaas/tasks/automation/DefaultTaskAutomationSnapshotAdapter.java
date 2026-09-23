package com.chacha.multitenantsaas.tasks.automation;

import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultTaskAutomationSnapshotAdapter implements TaskAutomationSnapshotPort {

    private final ProjectTaskRepository taskRepository;

    public DefaultTaskAutomationSnapshotAdapter(ProjectTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAutomationSnapshot snapshot(UUID tenantId, UUID projectId, UUID taskId) {
        ProjectTask task =
                taskRepository
                        .findByProject_Tenant_IdAndProject_IdAndId(tenantId, projectId, taskId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Task not found: " + taskId));
        return new TaskAutomationSnapshot(task.getStatus(), task.getPriority());
    }
}
