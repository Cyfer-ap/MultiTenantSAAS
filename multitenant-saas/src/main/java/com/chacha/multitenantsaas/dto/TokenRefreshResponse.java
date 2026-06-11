package com.chacha.multitenantsaas.dto;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        String message
) {
}