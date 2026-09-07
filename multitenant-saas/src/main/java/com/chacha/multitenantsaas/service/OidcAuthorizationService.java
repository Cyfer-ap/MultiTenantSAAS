package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.config.OidcLoginProperties;
import com.chacha.multitenantsaas.dto.OidcAuthorizationStartResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OidcAuthorizationService {

    private static final int RANDOM_BYTES = 32;

    private final OidcAuthorizationTransactionStateService stateService;
    private final OidcProviderMetadataService metadataService;
    private final IdentityProviderSecretCipher secretCipher;
    private final OidcLoginProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public OidcAuthorizationService(
            OidcAuthorizationTransactionStateService stateService,
            OidcProviderMetadataService metadataService,
            IdentityProviderSecretCipher secretCipher,
            OidcLoginProperties properties) {
        this.stateService = stateService;
        this.metadataService = metadataService;
        this.secretCipher = secretCipher;
        this.properties = properties;
    }

    public OidcAuthorizationStartResponse start(UUID tenantId, boolean persistentSession) {
        OidcProviderRuntimeSnapshot provider = stateService.loadVerifiedProvider(tenantId);
        OidcProviderMetadata metadata = metadataService.loadAndValidate(provider.issuerUri());

        // Fail before redirecting the browser if the server can no longer decrypt provider
        // credentials.
        secretCipher.decrypt(provider.clientSecretCiphertext());

        String state = OidcSecuritySupport.randomBase64Url(secureRandom, RANDOM_BYTES);
        String nonce = OidcSecuritySupport.randomBase64Url(secureRandom, RANDOM_BYTES);
        String pkceVerifier = OidcSecuritySupport.randomBase64Url(secureRandom, RANDOM_BYTES);
        String pkceChallenge = OidcSecuritySupport.pkceS256Challenge(pkceVerifier);
        String encryptedVerifier = secretCipher.encryptTransactionSecret(pkceVerifier);

        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(properties.authorizationTransactionTtl());
        stateService.createTransaction(
                provider,
                OidcSecuritySupport.sha256Hex(state),
                OidcSecuritySupport.sha256Hex(nonce),
                encryptedVerifier,
                persistentSession,
                createdAt,
                expiresAt);

        String authorizationUrl =
                UriComponentsBuilder.fromUriString(metadata.authorizationEndpoint())
                        .queryParam("response_type", "code")
                        .queryParam("client_id", provider.clientId())
                        .queryParam("redirect_uri", properties.requireRedirectUri().toString())
                        .queryParam("scope", String.join(" ", new TreeSet<>(provider.scopes())))
                        .queryParam("state", state)
                        .queryParam("nonce", nonce)
                        .queryParam("code_challenge", pkceChallenge)
                        .queryParam("code_challenge_method", "S256")
                        .build()
                        .encode()
                        .toUriString();

        return new OidcAuthorizationStartResponse(authorizationUrl, expiresAt);
    }
}
