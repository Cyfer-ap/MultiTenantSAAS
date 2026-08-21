package com.chacha.multitenantsaas.billing.provider;

public record BillingCheckoutSession(
        String sessionId, String checkoutUrl, BillingProviderType provider) {}
