package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record TenantOnboardingRequest(

        @NotBlank(message = "Tenant name is required")
        @Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
        String tenantName,

        @NotBlank(message = "Tenant slug is required")
        @Size(min = 2, max = 80, message = "Tenant slug must be between 2 and 80 characters")
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Tenant slug must contain only lowercase letters, numbers, and single hyphens; it cannot start or end with a hyphen"
        )
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
