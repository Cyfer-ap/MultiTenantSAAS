package com.chacha.multitenantsaas.dto;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken,
        String csrfToken,
        String tokenType,
        long expiresInSeconds,
        boolean persistentSession,
        String message) {

    public TokenRefreshResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds,
            String message) {
        this(accessToken, refreshToken, "", tokenType, expiresInSeconds, false, message);
    }
}
