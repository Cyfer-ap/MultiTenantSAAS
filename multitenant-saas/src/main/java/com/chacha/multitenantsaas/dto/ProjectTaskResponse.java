package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import java.time.Instant;
import java.util.UUID;

public record ProjectTaskResponse(
        UUID id,
        UUID tenantId,
        UUID projectId,
        String title,
        String description,
        ProjectTaskStatus status,
        ProjectTaskPriority priority,
        UUID assigneeUserId,
        String assigneeName,
        String assigneeEmail,
        UUID createdByUserId,
        String createdByUserName,
        String createdByUserEmail,
        Instant dueAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {}
