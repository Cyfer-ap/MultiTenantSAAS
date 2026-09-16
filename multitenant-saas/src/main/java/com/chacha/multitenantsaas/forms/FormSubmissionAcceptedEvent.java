package com.chacha.multitenantsaas.forms;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import java.util.UUID;

public record FormSubmissionAcceptedEvent(
        UUID tenantId,
        UUID projectId,
        UUID formId,
        UUID submissionId,
        UUID createdTaskId,
        UUID actorUserId,
        ProjectTaskPriority taskPriority,
        UUID workflowId) {}
