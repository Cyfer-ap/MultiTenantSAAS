package com.chacha.multitenantsaas.service;

import java.util.Set;
import java.util.UUID;

public record OidcProviderRuntimeSnapshot(
        UUID tenantId,
        UUID identityProviderId,
        long identityProviderVersion,
        String issuerUri,
        String clientId,
        String clientSecretCiphertext,
        Set<String> scopes,
        String displayName) {

    public OidcProviderRuntimeSnapshot {
        scopes = Set.copyOf(scopes);
    }
}
