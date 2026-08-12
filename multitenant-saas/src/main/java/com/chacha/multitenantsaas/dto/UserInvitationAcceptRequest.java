package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record UserInvitationAcceptRequest(
        @NotBlank(message = "Invitation token is required") String invitationToken,
        @StrongPassword String newPassword,
        @NotBlank(message = "Confirm password is required") String confirmPassword) {}
