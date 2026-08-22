package com.chacha.multitenantsaas.billing.webhook;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;

public record VerifiedBillingEvent(
        BillingProviderType provider, String providerEventId, String eventType, String payload) {}
