package com.chacha.multitenantsaas.externalaccess;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import java.time.Instant;
import java.util.UUID;

public record ExternalTaskSnapshot(
        UUID taskId,
        String title,
        String description,
        ProjectTaskStatus status,
        ProjectTaskPriority priority,
        Instant dueAt,
        Instant completedAt,
        Instant updatedAt) {}
