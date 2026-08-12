package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record SystemAdminResponse(
        UUID id,
        String fullName,
        String email,
        UserStatus status,
        int failedLoginAttempts,
        Instant lockedUntil,
        Instant createdAt,
        Instant updatedAt) {}
