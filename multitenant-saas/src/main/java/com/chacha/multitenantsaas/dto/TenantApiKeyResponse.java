package com.chacha.multitenantsaas.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantApiKeyResponse(
        UUID id,
        UUID tenantId,
        String name,
        String keyPrefix,
        boolean active,
        UUID createdByUserId,
        Instant expiresAt,
        Instant createdAt,
        Instant lastUsedAt,
        Instant revokedAt) {}
