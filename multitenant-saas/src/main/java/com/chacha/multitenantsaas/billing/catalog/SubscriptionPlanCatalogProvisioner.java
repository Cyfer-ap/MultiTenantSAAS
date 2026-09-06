package com.chacha.multitenantsaas.billing.catalog;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;

/** Provider-specific synchronization contract for externally managed billing catalogs. */
public interface SubscriptionPlanCatalogProvisioner {

    BillingProviderType providerType();

    void planCreated(SubscriptionPlanResponse plan);

    void planUpdated(SubscriptionPlanResponse before, SubscriptionPlanResponse after);
}
