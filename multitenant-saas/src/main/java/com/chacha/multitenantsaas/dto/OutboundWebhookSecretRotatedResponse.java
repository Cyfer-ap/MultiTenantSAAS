package com.chacha.multitenantsaas.dto;

import java.time.Instant;
import java.util.UUID;

public record OutboundWebhookSecretRotatedResponse(
        UUID endpointId,
        String secretHint,
        int secretVersion,
        String signingSecret,
        Instant secretRotatedAt) {}
