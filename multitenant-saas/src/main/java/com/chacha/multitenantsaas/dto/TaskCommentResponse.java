package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TaskCommentAuthorType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskCommentResponse(
        UUID id,
        UUID taskId,
        UUID parentCommentId,
        TaskCommentAuthorType authorType,
        UUID authorUserId,
        UUID externalAccessGrantId,
        String authorName,
        String authorEmail,
        String body,
        boolean deleted,
        int replyCount,
        boolean pinned,
        Instant pinnedAt,
        UUID pinnedByUserId,
        Instant editedAt,
        Instant deletedAt,
        Instant createdAt,
        Instant updatedAt,
        List<TaskCommentMentionResponse> mentions) {}
