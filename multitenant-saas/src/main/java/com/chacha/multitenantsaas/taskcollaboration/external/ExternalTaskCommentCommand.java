package com.chacha.multitenantsaas.taskcollaboration.external;

import java.util.UUID;

public record ExternalTaskCommentCommand(
        UUID tenantId,
        UUID projectId,
        UUID taskId,
        UUID grantId,
        String guestName,
        String guestEmail,
        String body) {}
