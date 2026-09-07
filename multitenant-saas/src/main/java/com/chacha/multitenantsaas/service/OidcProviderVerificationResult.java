package com.chacha.multitenantsaas.service;

public record OidcProviderVerificationResult(
        String issuer, String authorizationEndpoint, String tokenEndpoint, String jwkSetUri) {}
