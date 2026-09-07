package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.IdentityProviderProtocol;
import com.chacha.multitenantsaas.exception.IdentityProviderVerificationException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.stereotype.Service;

@Service
public class OidcProviderVerificationService {

    private final OidcProviderMetadataService metadataService;
    private final IdentityProviderSecretCipher secretCipher;

    public OidcProviderVerificationService(
            OidcProviderMetadataService metadataService,
            IdentityProviderSecretCipher secretCipher) {
        this.metadataService = metadataService;
        this.secretCipher = secretCipher;
    }

    public OidcProviderVerificationResult verify(OidcProviderVerificationInput input) {
        if (input.protocol() != IdentityProviderProtocol.OIDC) {
            throw new IdentityProviderVerificationException(
                    "Only OIDC identity providers can be verified by this runtime");
        }

        OidcProviderMetadata metadata = metadataService.loadAndValidate(input.issuerUri());
        String clientSecret = secretCipher.decrypt(input.clientSecretCiphertext());

        try {
            ClientRegistration registration =
                    ClientRegistrations.fromOidcConfiguration(metadata.configuration())
                            .registrationId("tenant-" + input.tenantId())
                            .clientId(input.clientId())
                            .clientSecret(clientSecret)
                            .scope(input.scopes())
                            .redirectUri("{baseUrl}/api/auth/oidc/callback/{registrationId}")
                            .clientName(input.displayName())
                            .build();

            if (!metadata.issuer().equals(registration.getProviderDetails().getIssuerUri())) {
                throw new IdentityProviderVerificationException(
                        "Spring OIDC client model resolved a different issuer than the validated provider");
            }
        } catch (IdentityProviderVerificationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IdentityProviderVerificationException(
                    "OIDC provider metadata is not compatible with the Spring Security client model",
                    exception);
        }

        return new OidcProviderVerificationResult(
                metadata.issuer(),
                metadata.authorizationEndpoint(),
                metadata.tokenEndpoint(),
                metadata.jwkSetUri());
    }
}
