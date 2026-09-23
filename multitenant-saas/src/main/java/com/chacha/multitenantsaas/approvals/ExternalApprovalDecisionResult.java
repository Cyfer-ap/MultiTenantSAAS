package com.chacha.multitenantsaas.approvals;

import java.time.Instant;
import java.util.UUID;

public record ExternalApprovalDecisionResult(
        UUID requestId,
        ApprovalRequestStatus status,
        int currentStageIndex,
        Instant completedAt) {}
