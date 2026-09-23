package com.chacha.multitenantsaas.approvals;

import java.util.UUID;

public record ExternalApprovalDecisionCommand(
        UUID tenantId,
        UUID projectId,
        UUID requestId,
        UUID grantId,
        ApprovalDecisionOutcome outcome,
        String comment) {}
