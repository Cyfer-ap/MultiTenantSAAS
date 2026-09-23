package com.chacha.multitenantsaas.externalaccess;

import com.chacha.multitenantsaas.entity.ProjectStatus;
import java.time.Instant;
import java.util.UUID;

public record ExternalProjectSnapshot(
        UUID projectId, String name, String description, ProjectStatus status, Instant updatedAt) {}
