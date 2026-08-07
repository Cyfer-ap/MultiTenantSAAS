package com.chacha.multitenantsaas.dto;

public enum SubscriptionAccessReason {
    ACTIVE,
    TRIAL_ACTIVE,
    PAST_DUE_GRACE,
    NO_SUBSCRIPTION,
    PLAN_INACTIVE,
    CANCELLED,
    EXPIRED,
    PERIOD_EXPIRED,
    TRIAL_EXPIRED
}
