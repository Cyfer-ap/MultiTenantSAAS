package com.chacha.multitenantsaas.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantApiKeyCreatedResponse(
        UUID id,
        UUID tenantId,
        String name,
        String keyPrefix,
        String apiKey,
        UUID createdByUserId,
        Instant expiresAt,
        Instant createdAt) {}
