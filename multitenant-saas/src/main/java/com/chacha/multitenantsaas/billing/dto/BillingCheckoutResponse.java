package com.chacha.multitenantsaas.billing.dto;

import com.chacha.multitenantsaas.billing.provider.BillingCheckoutSession;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;

public record BillingCheckoutResponse(
        String sessionId, String checkoutUrl, BillingProviderType provider) {

    public static BillingCheckoutResponse from(BillingCheckoutSession session) {
        return new BillingCheckoutResponse(
                session.sessionId(), session.checkoutUrl(), session.provider());
    }
}
