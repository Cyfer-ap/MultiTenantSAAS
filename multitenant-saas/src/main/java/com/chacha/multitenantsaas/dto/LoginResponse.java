package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserRole;

import java.util.UUID;

public record LoginResponse(
        UUID tenantId,
        UUID userId,
        String fullName,
        String email,
        UserRole role,
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        String message
) {
}