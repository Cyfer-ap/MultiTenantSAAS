package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required") String currentPassword,
        @StrongPassword String newPassword,
        @NotBlank(message = "Confirm password is required") String confirmPassword) {}
