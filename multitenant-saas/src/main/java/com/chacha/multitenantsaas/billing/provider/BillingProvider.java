package com.chacha.multitenantsaas.billing.provider;

import java.util.UUID;

public interface BillingProvider {

    BillingProviderType providerType();

    BillingCheckoutSession createCheckoutSession(UUID tenantId, String planCode);

    void cancelSubscription(String providerSubscriptionId);
}
