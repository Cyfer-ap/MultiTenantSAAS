package com.chacha.multitenantsaas.workflows;

public enum WorkflowExecutionStatus {
    RUNNING,
    WAITING_APPROVAL,
    SUCCEEDED,
    FAILED,
    SKIPPED
}
