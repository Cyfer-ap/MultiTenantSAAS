package com.chacha.multitenantsaas.billing.dto;

import com.chacha.multitenantsaas.billing.provider.BillingCancellationResult;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.util.UUID;

public record BillingCancellationResponse(
        UUID tenantId,
        BillingProviderType provider,
        String providerSubscriptionId,
        boolean cancellationRequested) {

    public static BillingCancellationResponse from(BillingCancellationResult result) {
        return new BillingCancellationResponse(
                result.tenantId(), result.provider(), result.providerSubscriptionId(), true);
    }
}
