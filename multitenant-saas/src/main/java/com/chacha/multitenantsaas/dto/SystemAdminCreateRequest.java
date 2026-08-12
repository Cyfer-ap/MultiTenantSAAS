package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SystemAdminCreateRequest(
        @NotBlank(message = "Full name is required") @Size(
                        min = 2,
                        max = 150,
                        message = "Full name must be between 2 and 150 characters")
                String fullName,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 150, message = "Email must not exceed 150 characters") String email,
        @StrongPassword String password) {}
