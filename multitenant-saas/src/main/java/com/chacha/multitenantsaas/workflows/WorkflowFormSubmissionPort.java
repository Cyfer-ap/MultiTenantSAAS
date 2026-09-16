package com.chacha.multitenantsaas.workflows;

import java.util.UUID;

public interface WorkflowFormSubmissionPort {

    void requireFormSubmissionTarget(UUID tenantId, UUID workflowId);

    void handleFormSubmission(WorkflowFormSubmissionCommand command);
}
