package com.chacha.multitenantsaas.billing.provider;

import java.util.UUID;

public record BillingCancellationResult(
        UUID tenantId, BillingProviderType provider, String providerSubscriptionId) {}
