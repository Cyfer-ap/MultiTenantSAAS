package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TaskComment;
import java.time.Instant;
import java.util.UUID;

public record OutboundWebhookCommentPayload(
        UUID id,
        UUID tenantId,
        UUID projectId,
        UUID taskId,
        UUID parentCommentId,
        UUID authorUserId,
        String body,
        Instant createdAt,
        Instant updatedAt) {

    public static OutboundWebhookCommentPayload from(TaskComment comment) {
        TaskComment parent = comment.getParentComment();
        return new OutboundWebhookCommentPayload(
                comment.getId(),
                comment.getTenant().getId(),
                comment.getProject().getId(),
                comment.getTask().getId(),
                parent == null ? null : parent.getId(),
                comment.getAuthorUser().getId(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
