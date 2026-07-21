package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record SystemAdminStatusUpdateRequest(

        @NotNull(message = "Status is required")
        UserStatus status
) {
}