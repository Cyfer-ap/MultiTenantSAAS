package com.chacha.multitenantsaas.taskrelationships.port;

import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectTask;
import java.util.List;
import java.util.UUID;

public interface TaskRelationshipTaskGateway {

    Project requireProject(UUID tenantId, UUID projectId);

    ProjectTask requireTask(UUID tenantId, UUID projectId, UUID taskId);

    List<ProjectTask> findChildren(UUID tenantId, UUID projectId, UUID parentTaskId, int limit);

    ProjectTask save(ProjectTask task);
}
