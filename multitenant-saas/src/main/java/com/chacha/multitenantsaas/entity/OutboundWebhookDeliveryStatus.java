package com.chacha.multitenantsaas.entity;

public enum OutboundWebhookDeliveryStatus {
    PENDING,
    PROCESSING,
    RETRY,
    SENT,
    FAILED
}
