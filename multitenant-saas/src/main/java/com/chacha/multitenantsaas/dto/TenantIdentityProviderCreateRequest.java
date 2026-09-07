package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.IdentityProviderProtocol;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record TenantIdentityProviderCreateRequest(
        @NotBlank @Size(max = 120) String displayName,
        @NotNull IdentityProviderProtocol protocol,
        @NotBlank @Size(max = 2048) String issuerUri,
        @NotBlank @Size(max = 512) String clientId,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
                @NotBlank
                @Size(min = 8, max = 2048)
                String clientSecret,
        Set<String> scopes) {}
