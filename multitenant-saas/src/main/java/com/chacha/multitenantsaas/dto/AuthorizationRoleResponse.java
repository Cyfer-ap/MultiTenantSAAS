package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.AuthorizationRoleSource;
import com.chacha.multitenantsaas.entity.AuthorizationRoleStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthorizationRoleResponse(

        UUID id,

        UUID tenantId,

        String code,

        String name,

        String description,

        AuthorizationRoleSource source,

        AuthorizationRoleStatus status,

        List<AuthorizationPermissionResponse> permissions,

        Instant createdAt,

        Instant updatedAt
) {
}