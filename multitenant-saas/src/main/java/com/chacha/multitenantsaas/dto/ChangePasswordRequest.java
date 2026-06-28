package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.chacha.multitenantsaas.validation.StrongPassword;

public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @StrongPassword
        String newPassword,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword
) {
}