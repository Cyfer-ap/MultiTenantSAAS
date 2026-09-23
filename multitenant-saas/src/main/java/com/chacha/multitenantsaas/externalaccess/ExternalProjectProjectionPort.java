package com.chacha.multitenantsaas.externalaccess;

import java.util.UUID;

public interface ExternalProjectProjectionPort {

    ExternalProjectSnapshot requireProject(UUID tenantId, UUID projectId);
}
