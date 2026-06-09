package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record AppUserResponse(
        UUID id,
        UUID tenantId,
        String fullName,
        String email,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}