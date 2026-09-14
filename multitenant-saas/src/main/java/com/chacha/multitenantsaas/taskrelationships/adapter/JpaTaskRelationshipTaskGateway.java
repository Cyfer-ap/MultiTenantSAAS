package com.chacha.multitenantsaas.taskrelationships.adapter;

import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipTaskGateway;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class JpaTaskRelationshipTaskGateway implements TaskRelationshipTaskGateway {

    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;

    public JpaTaskRelationshipTaskGateway(
            ProjectRepository projectRepository, ProjectTaskRepository projectTaskRepository) {
        this.projectRepository = projectRepository;
        this.projectTaskRepository = projectTaskRepository;
    }

    @Override
    public Project requireProject(UUID tenantId, UUID projectId) {
        return projectRepository
                .findByTenant_IdAndId(tenantId, projectId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Project not found with id: "
                                                + projectId
                                                + " for tenant: "
                                                + tenantId));
    }

    @Override
    public ProjectTask requireTask(UUID tenantId, UUID projectId, UUID taskId) {
        return projectTaskRepository
                .findByProject_Tenant_IdAndProject_IdAndId(tenantId, projectId, taskId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Task not found with id: "
                                                + taskId
                                                + " for project: "
                                                + projectId));
    }

    @Override
    public List<ProjectTask> findChildren(
            UUID tenantId, UUID projectId, UUID parentTaskId, int limit) {
        return projectTaskRepository
                .findByProject_Tenant_IdAndProject_IdAndParentTask_IdOrderByCreatedAtAsc(
                        tenantId, projectId, parentTaskId, PageRequest.of(0, limit));
    }

    @Override
    public ProjectTask save(ProjectTask task) {
        return projectTaskRepository.save(task);
    }
}
