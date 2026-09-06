package com.chacha.multitenantsaas.billing.entity;

/** Separates provider catalog references created in sandbox/test mode from live billing objects. */
public enum BillingProviderEnvironment {
    TEST,
    LIVE
}
