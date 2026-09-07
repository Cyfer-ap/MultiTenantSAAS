package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.exception.IdentityProviderVerificationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OidcProviderMetadataService {

    private final OidcHttpTransport httpTransport;
    private final IdentityProviderRemoteUriValidator remoteUriValidator;

    public OidcProviderMetadataService(
            OidcHttpTransport httpTransport, HostAddressResolver hostAddressResolver) {
        this.httpTransport = httpTransport;
        this.remoteUriValidator = new IdentityProviderRemoteUriValidator(hostAddressResolver);
    }

    public OidcProviderMetadata loadAndValidate(String configuredIssuer) {
        String issuer =
                remoteUriValidator.validateAndNormalize(
                        configuredIssuer, "Identity-provider issuer URI", false);
        URI discoveryUri = discoveryUri(issuer);

        remoteUriValidator.validateAndNormalize(
                discoveryUri.toString(), "OIDC discovery URI", false);
        Map<String, Object> configuration = httpTransport.getJson(discoveryUri);

        String metadataIssuer =
                remoteUriValidator.validateAndNormalize(
                        requiredString(configuration, "issuer"), "OIDC metadata issuer URI", false);
        if (!issuer.equals(metadataIssuer)) {
            throw new IdentityProviderVerificationException(
                    "OIDC metadata issuer does not exactly match the configured issuer");
        }

        String authorizationEndpoint =
                remoteUriValidator.validateAndNormalize(
                        requiredString(configuration, "authorization_endpoint"),
                        "OIDC authorization endpoint",
                        true);
        String tokenEndpoint =
                remoteUriValidator.validateAndNormalize(
                        requiredString(configuration, "token_endpoint"),
                        "OIDC token endpoint",
                        true);
        String jwkSetUri =
                remoteUriValidator.validateAndNormalize(
                        requiredString(configuration, "jwks_uri"), "OIDC JWKS endpoint", true);

        validateOptionalRemoteUri(configuration, "userinfo_endpoint", "OIDC userinfo endpoint");
        Map<String, Object> jwkSet = validateJwkSet(jwkSetUri);

        Map<String, Object> normalized = new LinkedHashMap<>(configuration);
        normalized.put("issuer", metadataIssuer);
        normalized.put("authorization_endpoint", authorizationEndpoint);
        normalized.put("token_endpoint", tokenEndpoint);
        normalized.put("jwks_uri", jwkSetUri);

        return new OidcProviderMetadata(
                normalized,
                metadataIssuer,
                authorizationEndpoint,
                tokenEndpoint,
                jwkSetUri,
                jwkSet);
    }

    private URI discoveryUri(String issuer) {
        String base = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
        return URI.create(base + "/.well-known/openid-configuration");
    }

    private Map<String, Object> validateJwkSet(String jwkSetUri) {
        String validated =
                remoteUriValidator.validateAndNormalize(jwkSetUri, "OIDC JWKS endpoint", true);
        Map<String, Object> jwks = httpTransport.getJson(URI.create(validated));
        Object keys = jwks.get("keys");
        if (!(keys instanceof List<?> keyList) || keyList.isEmpty()) {
            throw new IdentityProviderVerificationException(
                    "OIDC JWKS response must contain at least one signing key");
        }
        return jwks;
    }

    private void validateOptionalRemoteUri(
            Map<String, Object> configuration, String key, String label) {
        Object raw = configuration.get(key);
        if (raw == null) {
            return;
        }
        if (!(raw instanceof String value) || value.isBlank()) {
            throw new IdentityProviderVerificationException(
                    "OIDC metadata field " + key + " must be a non-blank URI string");
        }
        remoteUriValidator.validateAndNormalize(value, label, true);
    }

    private String requiredString(Map<String, Object> configuration, String key) {
        Object raw = configuration.get(key);
        if (!(raw instanceof String value) || value.isBlank()) {
            throw new IdentityProviderVerificationException(
                    "OIDC metadata is missing required field: " + key);
        }
        return value;
    }
}
