package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.NotBlank;
import com.chacha.multitenantsaas.validation.StrongPassword;

public record ResetPasswordRequest(

        @NotBlank(message = "Reset token is required")
        String resetToken,

        @StrongPassword
        String newPassword,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword
) {
}