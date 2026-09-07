package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record TenantIdentityProviderUpdateRequest(
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Size(max = 2048) String issuerUri,
        @NotBlank @Size(max = 512) String clientId,
        Set<String> scopes) {}
