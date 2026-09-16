package com.chacha.multitenantsaas.approvals;

import java.util.UUID;

public record ApprovalResolvedEvent(
        UUID requestId,
        UUID tenantId,
        UUID projectId,
        UUID workflowId,
        int workflowVersion,
        UUID workflowExecutionId,
        String workflowNodeKey,
        UUID taskId,
        UUID actorUserId,
        ApprovalRequestStatus outcome,
        String approvedNextNodeKey,
        String rejectedNextNodeKey) {}
