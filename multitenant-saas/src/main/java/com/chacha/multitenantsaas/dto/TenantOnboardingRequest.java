package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantOnboardingRequest(

        @NotBlank(message = "Tenant name is required")
        @Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
        String tenantName,

        @NotBlank(message = "Tenant slug is required")
        @Size(min = 2, max = 80, message = "Tenant slug must be between 2 and 80 characters")
        String tenantSlug,

        @NotBlank(message = "Admin full name is required")
        @Size(min = 2, max = 100, message = "Admin full name must be between 2 and 100 characters")
        String adminFullName,

        @NotBlank(message = "Admin email is required")
        @Email(message = "Admin email must be valid")
        String adminEmail,

        @NotBlank(message = "Admin password is required")
        @Size(min = 8, max = 100, message = "Admin password must be between 8 and 100 characters")
        String adminPassword
) {
}
