package com.chacha.multitenantsaas.tasks.external;

import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.externalaccess.ExternalTaskProjectionPort;
import com.chacha.multitenantsaas.externalaccess.ExternalTaskSnapshot;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskExternalProjectionAdapter implements ExternalTaskProjectionPort {

    private final ProjectTaskRepository projectTaskRepository;

    public TaskExternalProjectionAdapter(ProjectTaskRepository projectTaskRepository) {
        this.projectTaskRepository = projectTaskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalTaskSnapshot> listTasks(UUID tenantId, UUID projectId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        return projectTaskRepository
                .findProjectTasks(
                        tenantId,
                        projectId,
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(0, boundedLimit, Sort.by(Sort.Direction.ASC, "createdAt")))
                .getContent()
                .stream()
                .map(this::map)
                .toList();
    }

    private ExternalTaskSnapshot map(ProjectTask task) {
        return new ExternalTaskSnapshot(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueAt(),
                task.getCompletedAt(),
                task.getUpdatedAt());
    }
}
