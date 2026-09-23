package com.chacha.multitenantsaas.approvals;

import java.util.UUID;

public record ExternalApprovalDecisionCommand(
        UUID tenantId,
        UUID projectId,
        UUID requestId,
        UUID grantId,
        String guestName,
        String guestEmail,
        ApprovalDecisionOutcome outcome,
        String comment) {}
