package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record AppUserRoleUpdateRequest(

        @NotNull(message = "User role is required")
        UserRole role
) {
}

