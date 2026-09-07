package com.chacha.multitenantsaas.dto;

public record OutboundWebhookEndpointCreatedResponse(
        OutboundWebhookEndpointResponse endpoint, String signingSecret) {}
