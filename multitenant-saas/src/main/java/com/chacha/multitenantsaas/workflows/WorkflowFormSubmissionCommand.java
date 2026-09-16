package com.chacha.multitenantsaas.workflows;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import java.util.UUID;

public record WorkflowFormSubmissionCommand(
        UUID tenantId,
        UUID projectId,
        UUID workflowId,
        UUID formId,
        UUID submissionId,
        UUID taskId,
        UUID actorUserId,
        ProjectTaskPriority taskPriority) {}
