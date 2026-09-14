package com.chacha.multitenantsaas.tasks.events;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskDomainEvent(
        UUID eventId,
        TaskDomainEventType type,
        UUID tenantId,
        UUID projectId,
        UUID taskId,
        UUID actorUserId,
        ProjectTaskStatus previousStatus,
        ProjectTaskStatus status,
        ProjectTaskPriority priority,
        Instant occurredAt) {

    public static TaskDomainEvent created(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            UUID actorUserId,
            ProjectTaskStatus status,
            ProjectTaskPriority priority) {
        return new TaskDomainEvent(
                UUID.randomUUID(),
                TaskDomainEventType.CREATED,
                tenantId,
                projectId,
                taskId,
                actorUserId,
                null,
                status,
                priority,
                Instant.now());
    }

    public static TaskDomainEvent statusChanged(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            UUID actorUserId,
            ProjectTaskStatus previousStatus,
            ProjectTaskStatus status,
            ProjectTaskPriority priority) {
        return new TaskDomainEvent(
                UUID.randomUUID(),
                TaskDomainEventType.STATUS_CHANGED,
                tenantId,
                projectId,
                taskId,
                actorUserId,
                previousStatus,
                status,
                priority,
                Instant.now());
    }
}
