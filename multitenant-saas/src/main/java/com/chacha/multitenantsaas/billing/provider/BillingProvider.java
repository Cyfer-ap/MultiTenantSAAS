package com.chacha.multitenantsaas.billing.provider;

import java.util.UUID;

public interface BillingProvider {

    BillingProviderType providerType();

    BillingCheckoutSession createCheckoutSession(UUID tenantId, String planCode);

    BillingProviderSubscriptionSnapshot fetchSubscription(String providerSubscriptionId);

    void cancelSubscription(String providerSubscriptionId);
}
