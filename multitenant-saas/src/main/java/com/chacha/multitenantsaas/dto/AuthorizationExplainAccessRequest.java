package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record AuthorizationExplainAccessRequest(
        @NotNull(message = "Subject user id is required") UUID userId,
        @NotBlank(message = "Permission code is required") @Size(max = 120, message = "Permission code must be at most 120 characters") String permissionCode,
        @NotNull(message = "Access context type is required") AuthorizationAccessContextType contextType,
        UUID targetId,
        Instant effectiveAt) {}
