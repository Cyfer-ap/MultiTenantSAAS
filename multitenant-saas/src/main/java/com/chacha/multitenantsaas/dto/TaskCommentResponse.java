package com.chacha.multitenantsaas.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskCommentResponse(
        UUID id,
        UUID taskId,
        UUID authorUserId,
        String authorName,
        String authorEmail,
        String body,
        boolean deleted,
        Instant editedAt,
        Instant deletedAt,
        Instant createdAt,
        Instant updatedAt,
        List<TaskCommentMentionResponse> mentions) {}
