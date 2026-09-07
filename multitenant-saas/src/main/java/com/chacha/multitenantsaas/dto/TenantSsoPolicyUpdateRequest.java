package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TenantSsoMode;
import jakarta.validation.constraints.NotNull;

public record TenantSsoPolicyUpdateRequest(
        @NotNull(message = "SSO mode is required") TenantSsoMode ssoMode) {}
