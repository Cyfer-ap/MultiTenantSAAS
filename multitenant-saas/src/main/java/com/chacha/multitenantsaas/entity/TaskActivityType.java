package com.chacha.multitenantsaas.entity;

public enum TaskActivityType {
    TASK_CREATED,
    TASK_UPDATED,
    STATUS_CHANGED,
    ASSIGNEE_CHANGED,
    TASK_CANCELLED,
    COMMENT_ADDED,
    COMMENT_EDITED,
    COMMENT_DELETED,
    ATTACHMENT_ADDED,
    ATTACHMENT_DELETED
}
