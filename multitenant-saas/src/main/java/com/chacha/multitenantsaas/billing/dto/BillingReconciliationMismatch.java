package com.chacha.multitenantsaas.billing.dto;

public enum BillingReconciliationMismatch {
    PROVIDER,
    PROVIDER_SUBSCRIPTION_ID,
    PLAN_CODE,
    STATUS,
    CURRENT_PERIOD_START,
    CURRENT_PERIOD_END,
    CANCEL_AT_PERIOD_END
}
