package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TenantStatus;
import jakarta.validation.constraints.NotNull;

public record TenantStatusUpdateRequest(

        @NotNull(message = "Tenant status is required")
        TenantStatus status
) {
}
