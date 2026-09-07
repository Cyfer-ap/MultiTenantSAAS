package com.chacha.multitenantsaas.service;

import org.springframework.stereotype.Service;

@Service
public class IdentityProviderIssuerValidator {

    private final IdentityProviderRemoteUriValidator remoteUriValidator;

    public IdentityProviderIssuerValidator(HostAddressResolver hostAddressResolver) {
        this.remoteUriValidator = new IdentityProviderRemoteUriValidator(hostAddressResolver);
    }

    /**
     * Validates the configured issuer before it is persisted. OIDC metadata retrieval must repeat
     * the public-address check immediately before every server-side request so DNS rebinding cannot
     * bypass this policy.
     */
    public String validateAndNormalize(String value) {
        return remoteUriValidator.validateAndNormalize(
                value, "Identity-provider issuer URI", false);
    }
}
