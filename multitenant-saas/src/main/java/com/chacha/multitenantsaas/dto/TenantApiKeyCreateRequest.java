package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record TenantApiKeyCreateRequest(
        @NotBlank(message = "API key name is required")
                @Size(max = 100, message = "API key name must not exceed 100 characters")
                String name,
        @Future(message = "API key expiry must be in the future") Instant expiresAt) {}
