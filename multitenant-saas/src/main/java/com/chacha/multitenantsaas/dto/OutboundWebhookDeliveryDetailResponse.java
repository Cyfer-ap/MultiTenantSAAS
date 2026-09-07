package com.chacha.multitenantsaas.dto;

import java.util.List;

public record OutboundWebhookDeliveryDetailResponse(
        OutboundWebhookDeliveryResponse delivery,
        String payloadJson,
        List<OutboundWebhookDeliveryAttemptResponse> attempts) {}
