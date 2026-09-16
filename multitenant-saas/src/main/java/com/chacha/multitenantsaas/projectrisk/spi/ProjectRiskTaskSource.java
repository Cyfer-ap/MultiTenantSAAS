package com.chacha.multitenantsaas.projectrisk.spi;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProjectRiskTaskSource {

    List<TaskSnapshot> findProjectTasks(UUID tenantId, UUID projectId, int limit);

    record TaskSnapshot(
            UUID taskId,
            String title,
            String status,
            String priority,
            UUID assigneeUserId,
            Instant dueAt,
            Instant updatedAt) {}
}
