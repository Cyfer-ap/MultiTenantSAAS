package com.chacha.multitenantsaas.mywork.spi;

import java.time.Instant;
import java.util.UUID;

public record MyWorkTaskSnapshot(
        UUID taskId,
        UUID projectId,
        String title,
        String projectName,
        String status,
        String priority,
        Instant dueAt,
        Instant updatedAt) {}
