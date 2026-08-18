package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TaskAttachmentStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskAttachmentResponse(
        UUID id,
        UUID taskId,
        UUID commentId,
        UUID uploaderUserId,
        String uploaderName,
        String filename,
        String contentType,
        long sizeBytes,
        TaskAttachmentStatus status,
        Instant createdAt,
        Instant completedAt,
        Instant deletedAt) {}
