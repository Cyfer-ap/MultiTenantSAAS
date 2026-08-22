package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.provider.BillingCheckoutSession;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BillingCheckoutService {

    private final BillingProviderRegistry providerRegistry;

    public BillingCheckoutService(BillingProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    public BillingCheckoutSession createCheckoutSession(
            UUID tenantId, String planCode, BillingProviderType providerType) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(providerType, "providerType must not be null");
        if (planCode == null || planCode.isBlank()) {
            throw new IllegalArgumentException("planCode must not be blank");
        }

        return providerRegistry
                .require(providerType)
                .createCheckoutSession(tenantId, planCode.trim());
    }
}
