package com.chacha.multitenantsaas.service;

import java.util.Map;

public record OidcProviderMetadata(
        Map<String, Object> configuration,
        String issuer,
        String authorizationEndpoint,
        String tokenEndpoint,
        String jwkSetUri) {

    public OidcProviderMetadata {
        configuration = Map.copyOf(configuration);
    }
}
