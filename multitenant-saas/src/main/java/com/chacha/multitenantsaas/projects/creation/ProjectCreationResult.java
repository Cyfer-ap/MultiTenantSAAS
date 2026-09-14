package com.chacha.multitenantsaas.projects.creation;

import com.chacha.multitenantsaas.entity.ProjectStatus;
import java.time.Instant;
import java.util.UUID;

public record ProjectCreationResult(
        UUID projectId,
        UUID tenantId,
        String name,
        String description,
        ProjectStatus status,
        UUID createdByUserId,
        String createdByUserName,
        String createdByUserEmail,
        Instant createdAt,
        Instant updatedAt) {}
