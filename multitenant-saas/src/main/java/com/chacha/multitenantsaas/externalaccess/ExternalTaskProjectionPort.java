package com.chacha.multitenantsaas.externalaccess;

import java.util.List;
import java.util.UUID;

public interface ExternalTaskProjectionPort {

    List<ExternalTaskSnapshot> listTasks(UUID tenantId, UUID projectId, int limit);
}
