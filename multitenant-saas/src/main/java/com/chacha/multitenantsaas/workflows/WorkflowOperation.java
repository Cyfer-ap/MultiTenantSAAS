package com.chacha.multitenantsaas.workflows;

public enum WorkflowOperation {
    TRIGGER_TASK_CREATED(WorkflowNodeType.TRIGGER, ConfigurationKind.NONE),
    TRIGGER_TASK_STATUS_CHANGED(WorkflowNodeType.TRIGGER, ConfigurationKind.NONE),
    TRIGGER_FORM_SUBMITTED(WorkflowNodeType.TRIGGER, ConfigurationKind.NONE),
    CONDITION_TASK_PRIORITY_EQUALS(WorkflowNodeType.CONDITION, ConfigurationKind.PRIORITY),
    CONDITION_TASK_STATUS_EQUALS(WorkflowNodeType.CONDITION, ConfigurationKind.STATUS),
    ACTION_SET_TASK_PRIORITY(WorkflowNodeType.ACTION, ConfigurationKind.PRIORITY),
    ACTION_SET_TASK_STATUS(WorkflowNodeType.ACTION, ConfigurationKind.STATUS),
    ACTION_REQUEST_APPROVAL(WorkflowNodeType.ACTION, ConfigurationKind.APPROVAL_DEFINITION);

    private final WorkflowNodeType nodeType;
    private final ConfigurationKind configurationKind;

    WorkflowOperation(WorkflowNodeType nodeType, ConfigurationKind configurationKind) {
        this.nodeType = nodeType;
        this.configurationKind = configurationKind;
    }

    public WorkflowNodeType nodeType() {
        return nodeType;
    }

    ConfigurationKind configurationKind() {
        return configurationKind;
    }

    enum ConfigurationKind {
        NONE,
        PRIORITY,
        STATUS,
        APPROVAL_DEFINITION
    }
}
