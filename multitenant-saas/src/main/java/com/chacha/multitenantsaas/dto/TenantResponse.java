package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TenantStatus;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        TenantStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
