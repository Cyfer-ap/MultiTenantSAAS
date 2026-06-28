package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.chacha.multitenantsaas.validation.StrongPassword;

public record AppUserCreateRequest(

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @StrongPassword
        String password,

        @NotNull(message = "User role is required")
        UserRole role
) {
}