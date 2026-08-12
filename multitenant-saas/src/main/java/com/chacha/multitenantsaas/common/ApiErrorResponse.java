package com.chacha.multitenantsaas.common;

import java.time.Instant;

public record ApiErrorResponse(
        boolean success,
        String message,
        ErrorCode errorCode,
        int status,
        String path,
        Object details,
        Instant timestamp) {

    public static ApiErrorResponse of(
            String message, ErrorCode errorCode, int status, String path, Object details) {
        return new ApiErrorResponse(
                false, message, errorCode, status, path, details, Instant.now());
    }
}
