package com.chacha.multitenantsaas.service;

import java.util.Set;
import java.util.UUID;

public record OidcCallbackTransactionSnapshot(
        UUID tenantId,
        UUID identityProviderId,
        long identityProviderVersion,
        String issuerUri,
        String clientId,
        String clientSecretCiphertext,
        Set<String> scopes,
        String displayName,
        String nonceHash,
        String pkceVerifierCiphertext,
        boolean persistentSession) {

    public OidcCallbackTransactionSnapshot {
        scopes = Set.copyOf(scopes);
    }
}
