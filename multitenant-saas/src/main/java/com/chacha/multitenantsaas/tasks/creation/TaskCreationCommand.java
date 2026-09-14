package com.chacha.multitenantsaas.tasks.creation;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import java.time.Instant;
import java.util.UUID;

public record TaskCreationCommand(
        UUID tenantId,
        UUID projectId,
        UUID creatorUserId,
        UUID assigneeUserId,
        String title,
        String description,
        ProjectTaskPriority priority,
        Instant dueAt,
        String activitySummary) {}
