package com.chacha.multitenantsaas.entity;

public enum TenantSubscriptionHistoryEventType {
    MIGRATED_CURRENT_STATE,
    STARTED,
    PLAN_CHANGED,
    LIFECYCLE_UPDATED,
    PROVIDER_SYNCHRONIZED,
    PROVIDER_RECONCILED
}
