package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BillingProviderRegistry {

    private final Map<BillingProviderType, BillingProvider> providers;

    public BillingProviderRegistry(List<BillingProvider> providers) {
        EnumMap<BillingProviderType, BillingProvider> registered =
                new EnumMap<>(BillingProviderType.class);
        for (BillingProvider provider : providers) {
            BillingProvider existing = registered.putIfAbsent(provider.providerType(), provider);
            if (existing != null) {
                throw new IllegalStateException(
                        "Multiple billing providers registered for " + provider.providerType());
            }
        }
        this.providers = Map.copyOf(registered);
    }

    public List<BillingProviderType> availableProviderTypes() {
        return providers.keySet().stream().sorted().toList();
    }

    public BillingProvider require(BillingProviderType providerType) {
        BillingProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new IllegalArgumentException(
                    "Billing provider is not configured: " + providerType);
        }
        return provider;
    }
}
