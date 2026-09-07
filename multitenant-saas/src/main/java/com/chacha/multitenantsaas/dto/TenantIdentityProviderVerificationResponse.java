package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import java.time.Instant;
import java.util.UUID;

public record TenantIdentityProviderVerificationResponse(
        UUID id,
        TenantIdentityProviderStatus status,
        String issuerUri,
        String authorizationEndpoint,
        String tokenEndpoint,
        String jwkSetUri,
        Instant verifiedAt) {}
