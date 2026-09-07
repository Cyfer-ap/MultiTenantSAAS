package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.IdentityProviderProtocol;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantSsoMode;
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
        TenantSsoMode ssoMode,
        String clientSecretHint,
        int secretVersion,
        Instant verifiedAt,
        Instant disabledAt,
        Instant secretRotatedAt,
        Instant createdAt,
        Instant updatedAt) {}
