package com.chacha.multitenantsaas.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum OutboundWebhookEventType {
    PROJECT_CREATED("project.created"),
    PROJECT_UPDATED("project.updated"),
    PROJECT_ARCHIVED("project.archived"),
    TASK_CREATED("task.created"),
    TASK_UPDATED("task.updated"),
    TASK_COMPLETED("task.completed"),
    COMMENT_CREATED("comment.created"),
    COMMENT_REPLIED("comment.replied"),
    MEMBER_ADDED("member.added"),
    MEMBER_REMOVED("member.removed"),
    SUBSCRIPTION_UPDATED("subscription.updated"),
    SUBSCRIPTION_CANCELLED("subscription.cancelled");

    private final String wireName;

    OutboundWebhookEventType(String wireName) {
        this.wireName = wireName;
    }

    @JsonValue
    public String wireName() {
        return wireName;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OutboundWebhookEventType fromWireName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Webhook event type must not be null");
        }
        return Arrays.stream(values())
                .filter(type -> type.wireName.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported webhook event type: " + value));
    }
}
