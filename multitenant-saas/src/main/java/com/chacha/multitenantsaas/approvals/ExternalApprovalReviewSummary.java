package com.chacha.multitenantsaas.approvals;

import java.time.Instant;
import java.util.UUID;

public record ExternalApprovalReviewSummary(
        UUID requestId,
        UUID requestStageId,
        UUID taskId,
        String stageName,
        Instant createdAt) {}
