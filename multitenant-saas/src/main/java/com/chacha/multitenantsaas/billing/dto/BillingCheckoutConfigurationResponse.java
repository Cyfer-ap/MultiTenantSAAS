package com.chacha.multitenantsaas.billing.dto;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import java.util.List;

public record BillingCheckoutConfigurationResponse(
        List<SubscriptionPlanResponse> plans, List<BillingProviderType> providers) {

    public BillingCheckoutConfigurationResponse {
        plans = List.copyOf(plans);
        providers = List.copyOf(providers);
    }
}
