package com.chacha.multitenantsaas.billing.provider;

import java.util.UUID;

public interface BillingProvider {

    BillingProviderType providerType();

    BillingCheckoutSession createCheckoutSession(UUID tenantId, String planCode);

    BillingProviderSubscriptionSnapshot fetchSubscription(String providerSubscriptionId);

    /**
     * Verifies that this provider owns the supplied provider subscription ID without mutating the
     * subscription.
     *
     * <p>Providers may override this with a lightweight existence/identity lookup when a complete
     * subscription snapshot requires lifecycle fields that are irrelevant to ownership recovery.
     */
    default boolean ownsSubscription(String providerSubscriptionId) {
        BillingProviderSubscriptionSnapshot snapshot = fetchSubscription(providerSubscriptionId);
        return snapshot != null
                && snapshot.provider() == providerType()
                && providerSubscriptionId != null
                && providerSubscriptionId.trim().equals(snapshot.providerSubscriptionId());
    }

    void cancelSubscription(String providerSubscriptionId);

    /**
     * Schedules cancellation for the end of the already-paid billing period. Providers whose normal
     * cancellation method already has this behavior may use the default implementation.
     */
    default void scheduleCancellationAtPeriodEnd(String providerSubscriptionId) {
        cancelSubscription(providerSubscriptionId);
    }
}
