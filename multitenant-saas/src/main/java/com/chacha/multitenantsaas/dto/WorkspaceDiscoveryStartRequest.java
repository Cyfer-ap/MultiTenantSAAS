package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceDiscoveryStartRequest(
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 150, message = "Email must be at most 150 characters") String email,
        @Size(max = 200, message = "Trusted browser token is invalid") String trustedBrowserToken) {}
