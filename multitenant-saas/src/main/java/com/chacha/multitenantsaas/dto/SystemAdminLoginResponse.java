package com.chacha.multitenantsaas.dto;

import java.util.UUID;

public record SystemAdminLoginResponse(
        UUID systemAdminId,
        String fullName,
        String email,
        String role,
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String message
) {
}