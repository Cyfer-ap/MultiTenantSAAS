package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.entity.UserRole;

import java.util.UUID;

public record AuthenticatedUserContext(
        UUID tenantId,
        UUID userId,
        String email,
        String fullName,
        UserRole role
) {
}