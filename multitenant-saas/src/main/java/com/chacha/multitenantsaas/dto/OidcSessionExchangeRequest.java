package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OidcSessionExchangeRequest(
        @NotBlank(message = "OIDC session handoff code is required") @Size(max = 256, message = "OIDC session handoff code is invalid") String code) {}
