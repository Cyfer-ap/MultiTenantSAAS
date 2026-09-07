package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.exception.IdentityProviderVerificationException;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OidcProviderMetadataServiceTest {

    private static final String ISSUER = "https://idp.example.com";

    @Test
    void validatesDiscoveryIssuerEndpointsAndJwks() throws Exception {
        OidcProviderMetadataService service =
                new OidcProviderMetadataService(validTransport(ISSUER), publicResolver());

        OidcProviderMetadata metadata = service.loadAndValidate(ISSUER);

        assertThat(metadata.issuer()).isEqualTo(ISSUER);
        assertThat(metadata.authorizationEndpoint()).isEqualTo(ISSUER + "/authorize");
        assertThat(metadata.tokenEndpoint()).isEqualTo(ISSUER + "/token");
        assertThat(metadata.jwkSetUri()).isEqualTo(ISSUER + "/jwks");
    }

    @Test
    void rejectsMetadataWhoseIssuerDoesNotExactlyMatchConfiguration() throws Exception {
        OidcHttpTransport transport =
                uri -> {
                    if (uri.getPath().endsWith("/.well-known/openid-configuration")) {
                        return configuration("https://different.example.com");
                    }
                    return Map.of("keys", List.of(Map.of("kty", "RSA", "kid", "key-1")));
                };
        OidcProviderMetadataService service =
                new OidcProviderMetadataService(transport, publicResolver());

        assertThatThrownBy(() -> service.loadAndValidate(ISSUER))
                .isInstanceOf(IdentityProviderVerificationException.class)
                .hasMessage("OIDC metadata issuer does not exactly match the configured issuer");
    }

    @Test
    void rejectsJwksWithoutSigningKeys() throws Exception {
        OidcHttpTransport transport =
                uri -> {
                    if (uri.getPath().endsWith("/.well-known/openid-configuration")) {
                        return configuration(ISSUER);
                    }
                    return Map.of("keys", List.of());
                };
        OidcProviderMetadataService service =
                new OidcProviderMetadataService(transport, publicResolver());

        assertThatThrownBy(() -> service.loadAndValidate(ISSUER))
                .isInstanceOf(IdentityProviderVerificationException.class)
                .hasMessage("OIDC JWKS response must contain at least one signing key");
    }

    private OidcHttpTransport validTransport(String issuer) {
        return uri -> {
            if (uri.getPath().endsWith("/.well-known/openid-configuration")) {
                return configuration(issuer);
            }
            return Map.of("keys", List.of(Map.of("kty", "RSA", "kid", "key-1")));
        };
    }

    private Map<String, Object> configuration(String issuer) {
        return Map.of(
                "issuer", issuer,
                "authorization_endpoint", issuer + "/authorize",
                "token_endpoint", issuer + "/token",
                "jwks_uri", issuer + "/jwks",
                "response_types_supported", List.of("code"),
                "subject_types_supported", List.of("public"),
                "id_token_signing_alg_values_supported", List.of("RS256"));
    }

    private HostAddressResolver publicResolver() throws Exception {
        InetAddress publicAddress = InetAddress.getByName("8.8.8.8");
        return host -> List.of(publicAddress);
    }
}
