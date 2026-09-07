package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import java.time.Instant;
import java.util.UUID;

public record TenantIdentityProviderSecretRotatedResponse(
        UUID id,
        String clientSecretHint,
        int secretVersion,
        TenantIdentityProviderStatus status,
        Instant secretRotatedAt) {}
