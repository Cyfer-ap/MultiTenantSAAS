package com.chacha.multitenantsaas.tasks.automation;

import java.util.UUID;

public record TaskAutomationMutationCommand(
        UUID tenantId,
        UUID projectId,
        UUID taskId,
        UUID actorUserId,
        UUID workflowId,
        UUID workflowExecutionId,
        TaskAutomationMutationType mutationType,
        String value) {}
