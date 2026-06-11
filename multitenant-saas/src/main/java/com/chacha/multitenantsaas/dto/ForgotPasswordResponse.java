package com.chacha.multitenantsaas.dto;

public record ForgotPasswordResponse(
        String message,
        String devResetToken
) {
}