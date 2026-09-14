package com.chacha.multitenantsaas.projects.creation;

import com.chacha.multitenantsaas.entity.ProjectStatus;
import java.util.UUID;

public record ProjectCreationCommand(
        UUID tenantId,
        UUID actorUserId,
        String name,
        String description,
        ProjectStatus initialStatus) {}
