package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserStatus;

import java.util.UUID;

public record SystemAdminCurrentResponse(
        UUID systemAdminId,
        String fullName,
        String email,
        String role,
        UserStatus status
) {
}