package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantCreateRequest(

        @NotBlank(message = "Tenant name is required")
        @Size(min = 2, max = 150, message = "Tenant name must be between 2 and 150 characters")
        String name,

        @NotBlank(message = "Tenant slug is required")
        @Size(min = 2, max = 100, message = "Tenant slug must be between 2 and 100 characters")
        String slug
) {
}

