package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserInvitationCreateRequest(
        @NotBlank(message = "Full name is required") @Size(min = 2, max = 150) String fullName,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 150) String email,
        @NotNull(message = "Role is required") UserRole role) {}
