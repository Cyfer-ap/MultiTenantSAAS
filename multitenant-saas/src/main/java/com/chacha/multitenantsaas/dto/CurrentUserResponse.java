package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;

import java.util.UUID;

public record CurrentUserResponse(
        UUID tenantId,
        String tenantName,
        String tenantSlug,
        UUID userId,
        String fullName,
        String email,
        UserRole role,
        UserStatus status
) {
}