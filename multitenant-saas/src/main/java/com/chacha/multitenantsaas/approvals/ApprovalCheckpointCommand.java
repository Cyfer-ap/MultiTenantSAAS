package com.chacha.multitenantsaas.approvals;

import java.util.UUID;

public record ApprovalCheckpointCommand(
        UUID tenantId,
        UUID projectId,
        UUID approvalDefinitionId,
        UUID workflowId,
        int workflowVersion,
        UUID workflowExecutionId,
        String workflowNodeKey,
        UUID taskId,
        UUID actorUserId,
        String approvedNextNodeKey,
        String rejectedNextNodeKey) {}
