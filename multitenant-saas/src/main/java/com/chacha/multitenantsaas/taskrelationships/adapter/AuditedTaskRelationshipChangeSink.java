package com.chacha.multitenantsaas.taskrelationships.adapter;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.TaskActivityType;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.service.AuditLogService;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.service.TaskActivityService;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipChangeSink;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuditedTaskRelationshipChangeSink implements TaskRelationshipChangeSink {

    private final CurrentActorService currentActorService;
    private final AuditLogService auditLogService;
    private final TaskActivityService taskActivityService;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;

    public AuditedTaskRelationshipChangeSink(
            CurrentActorService currentActorService,
            AuditLogService auditLogService,
            TaskActivityService taskActivityService,
            ProjectRepository projectRepository,
            ProjectTaskRepository projectTaskRepository) {
        this.currentActorService = currentActorService;
        this.auditLogService = auditLogService;
        this.taskActivityService = taskActivityService;
        this.projectRepository = projectRepository;
        this.projectTaskRepository = projectTaskRepository;
    }

    @Override
    public void record(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            Jwt jwt,
            AuditAction auditAction,
            TaskActivityType activityType,
            String summary) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        Project project =
                projectRepository
                        .findByTenant_IdAndId(tenantId, projectId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Project not found with id: " + projectId));

        if (taskId != null && activityType != null) {
            ProjectTask task =
                    projectTaskRepository
                            .findByProject_Tenant_IdAndProject_IdAndId(
                                    tenantId, projectId, taskId)
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Task not found with id: " + taskId));
            taskActivityService.record(task, actor, activityType, summary);
        }

        auditLogService.recordSelfSuccess(project.getTenant(), actor, auditAction, summary);
    }
}
