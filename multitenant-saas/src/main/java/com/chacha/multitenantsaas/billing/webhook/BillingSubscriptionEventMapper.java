package com.chacha.multitenantsaas.billing.webhook;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.util.Optional;

public interface BillingSubscriptionEventMapper {

    BillingProviderType providerType();

    Optional<BillingSubscriptionUpdate> map(VerifiedBillingEvent event);
}
