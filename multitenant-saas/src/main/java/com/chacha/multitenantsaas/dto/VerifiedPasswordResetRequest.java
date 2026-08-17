package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VerifiedPasswordResetRequest(
        @NotNull(message = "Workspace is required") UUID tenantId,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        @NotNull(message = "Email verification is required") UUID workspaceGrantId) {}
