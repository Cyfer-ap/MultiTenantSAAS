package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.IdentityProviderProtocol;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TenantIdentityProviderResponse(
        UUID id,
        UUID tenantId,
        IdentityProviderProtocol protocol,
        String displayName,
        String issuerUri,
        String clientId,
        Set<String> scopes,
        TenantIdentityProviderStatus status,
        String clientSecretHint,
        int secretVersion,
        Instant verifiedAt,
        Instant disabledAt,
        Instant secretRotatedAt,
        Instant createdAt,
        Instant updatedAt) {}
