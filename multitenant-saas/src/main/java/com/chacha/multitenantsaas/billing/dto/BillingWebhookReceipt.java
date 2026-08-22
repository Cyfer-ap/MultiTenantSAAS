package com.chacha.multitenantsaas.billing.dto;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;

public record BillingWebhookReceipt(
        BillingProviderType provider,
        String providerEventId,
        String eventType,
        boolean duplicate) {}
