package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionStatus;

import java.time.Instant;
import java.util.UUID;

public record AuthorizationPermissionResponse(

        UUID id,

        UUID tenantId,

        String code,

        String name,

        String description,

        String category,

        AuthorizationPermissionSource source,

        AuthorizationPermissionStatus status,

        Instant createdAt,

        Instant updatedAt
) {
}