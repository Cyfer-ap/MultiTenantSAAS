package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required") String resetToken,
        @StrongPassword String newPassword,
        @NotBlank(message = "Confirm password is required") String confirmPassword) {}
