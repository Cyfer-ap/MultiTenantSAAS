package com.chacha.multitenantsaas.taskcollaboration.external;

import java.time.Instant;
import java.util.UUID;

public record ExternalTaskCommentSnapshot(
        UUID id,
        UUID taskId,
        UUID grantId,
        String guestName,
        String guestEmail,
        String body,
        Instant createdAt) {}
