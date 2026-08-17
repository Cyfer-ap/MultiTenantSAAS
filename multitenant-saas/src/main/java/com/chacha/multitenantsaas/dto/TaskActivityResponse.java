package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TaskActivityType;
import java.time.Instant;
import java.util.UUID;

public record TaskActivityResponse(
        UUID id,
        UUID taskId,
        TaskActivityType type,
        UUID actorUserId,
        String actorName,
        String actorEmail,
        String summary,
        Instant createdAt) {}
