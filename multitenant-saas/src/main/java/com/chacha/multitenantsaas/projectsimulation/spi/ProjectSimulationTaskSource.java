package com.chacha.multitenantsaas.projectsimulation.spi;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectSimulationTaskSource {

    List<TaskSnapshot> findProjectTasks(UUID tenantId, UUID projectId, int limit);

    Optional<AssigneeSnapshot> findAssignableAssignee(
            UUID tenantId, UUID projectId, UUID userId);

    record TaskSnapshot(
            UUID taskId,
            String title,
            String status,
            UUID assigneeUserId,
            String assigneeName,
            Instant dueAt) {}

    record AssigneeSnapshot(UUID userId, String displayName) {}
}
