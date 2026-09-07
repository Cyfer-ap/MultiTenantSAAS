package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.IdentityProviderProtocol;
import java.util.Set;
import java.util.UUID;

public record OidcProviderVerificationInput(
        UUID tenantId,
        IdentityProviderProtocol protocol,
        String issuerUri,
        String clientId,
        String clientSecretCiphertext,
        Set<String> scopes,
        String displayName) {}
