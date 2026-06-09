package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record AppUserStatusUpdateRequest(

        @NotNull(message = "User status is required")
        UserStatus status
) {
}
