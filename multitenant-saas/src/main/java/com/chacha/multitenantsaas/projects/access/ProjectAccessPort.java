package com.chacha.multitenantsaas.projects.access;

import java.util.UUID;

public interface ProjectAccessPort {

    ProjectAccessSnapshot requireProject(UUID tenantId, UUID projectId);
}
