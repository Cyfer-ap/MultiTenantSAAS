package com.chacha.multitenantsaas.mywork.spi;

import java.util.List;
import java.util.UUID;

public interface MyWorkTaskSource {

    List<MyWorkTaskSnapshot> findAssignedOpenTasks(UUID tenantId, UUID userId, int limit);
}
