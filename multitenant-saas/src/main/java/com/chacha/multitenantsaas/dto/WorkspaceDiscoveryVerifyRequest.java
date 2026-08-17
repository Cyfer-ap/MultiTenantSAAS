package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record WorkspaceDiscoveryVerifyRequest(
        @NotNull(message = "Challenge ID is required") UUID challengeId,
        @NotBlank(message = "Verification code is required") @Pattern(regexp = "\\d{6}", message = "Verification code must contain 6 digits") String code,
        boolean trustBrowser) {}
